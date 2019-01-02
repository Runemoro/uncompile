package uncompile.transformation;

import uncompile.DecompilationNotPossibleException;
import uncompile.ast.Class;
import uncompile.ast.*;
import uncompile.metadata.ClassType;
import uncompile.metadata.ReferenceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixInnerClassesTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        // Transform $ to .
        new AstVisitor() {
            @Override
            public void visit(ClassReference classReference) {
                super.visit(classReference);
                while (classReference.className.contains("$")) {
                    int dollarIndex = classReference.className.indexOf('$');
                    classReference.parent = new ClassReference(classReference.parent, classReference.className.substring(0, dollarIndex));
                    classReference.className = classReference.className.substring(dollarIndex + 1);
                }
            }
        }.visit(clazz);

        // Get information about outer this fields and params
        Map<VariableDeclaration, ThisReference> outerThisParams = new HashMap<>();
        Map<String, ThisReference> outerThisFields = new HashMap<>(); // fully qualified field name -> this type

        new AstVisitor() {
            private Class currentClass = null;

            @Override
            public void visit(Class clazz) {
                Class oldClass = currentClass;
                currentClass = clazz;
                super.visit(clazz);
                currentClass = oldClass;
            }

            @Override
            public void visit(Method method) {
                if (currentClass.outerClass == null) {
                    return;
                }

                checkOuterThisInConstructor(method);
                checkAccessor(method);

                // After outerThisParam is determined
                super.visit(method);
            }

            private void checkOuterThisInConstructor(Method method) {
                if (method.name.equals("<init>") && !currentClass.isStatic) {
                    if (method.parameters.isEmpty()) {
                        throw new DecompilationNotPossibleException("inner class constructor doesn't have an outer this param");
                    }

                    VariableDeclaration outerThisParam = method.parameters.get(0);
                    ClassType outerType = currentClass.outerClass.getClassType();
                    if (!(outerThisParam.type instanceof ReferenceTypeNode) || !((ReferenceType) outerThisParam.getType()).getRawType().equals(outerType)) {
                        ClassType type = ((ReferenceType) outerThisParam.getType()).getRawType();
                        throw new DecompilationNotPossibleException("inner class constructor super param is of wrong type: " + type);
                    }

                    ClassReference refToOuter = new ClassReference(currentClass.outerClass.getClassType());
                    // Thankfully Java guarantees there aren't two classes named the same in the same file
                    refToOuter.isQualified = false;
                    outerThisParams.put(outerThisParam, new ThisReference(refToOuter, true));
                }
            }

            private void checkAccessor(Method method) {
                // TODO
            }

            @Override
            public void visit(Assignment assignment) {
                super.visit(assignment);

                if (assignment.left instanceof InstanceFieldReference && assignment.right instanceof VariableReference) {
                    InstanceFieldReference fieldReference = (InstanceFieldReference) assignment.left;
                    VariableDeclaration variable = ((VariableReference) assignment.right).declaration;

                    ThisReference outerThisReference = outerThisParams.get(variable.declaration);
                    if (outerThisReference != null) {
                        if (!(fieldReference.target instanceof ThisReference &&
                              ((ThisReference) fieldReference.target).owner.getFullName().equals(currentClass.getFullName()))) {
                            throw new DecompilationNotPossibleException("a field from another class is being set to an outer this param");
                        }

                        Field outerThisField = currentClass.getFieldByName(fieldReference.field.getName());

                        if (outerThisField == null) {
                            throw new DecompilationNotPossibleException("referenced field does not exist");
                        }

                        outerThisFields.put(outerThisField.owner.getFullName() + "." + fieldReference.field.getName(), outerThisReference);
                    }
                }
            }
        }.visit(clazz);

        // Replace synthetic fields and methods
        new TransformingAstVisitor() {
            @Override
            public Expression transform(Expression expression) {
                if (expression instanceof InstanceFieldReference) {
                    InstanceFieldReference fieldReference = (InstanceFieldReference) expression;
                    String qualifiedFieldName = ((ReferenceType) fieldReference.target.getType()).getRawType().getFullName() + "." + fieldReference.field;
                    ThisReference outerThisReference = outerThisFields.get(qualifiedFieldName);

                    if (outerThisReference != null) {
                        return outerThisReference;
                    }
                }

                if (expression instanceof Assignment) {
                    Assignment assignment = (Assignment) expression;
                    if (assignment.left instanceof VariableReference) {
                        if (outerThisParams.get(((VariableReference) assignment.left).declaration) != null) {
                            return null;
                        }
                    } else if (assignment.left instanceof InstanceFieldReference) {
                        InstanceFieldReference fieldReference = (InstanceFieldReference) assignment.left;
                        String qualifiedFIeldName = ((ReferenceType) fieldReference.target.getType()).getRawType().getFullName() + "." + fieldReference.field;
                        ThisReference outerThisReference = outerThisFields.get(qualifiedFIeldName);

                        if (outerThisReference != null) {
                            return null;
                        }
                    }
                }

                return expression;
            }

            @Override
            public void visit(Class clazz) {
                // Visit class before removing outer this fields
                super.visit(clazz);

                // Remove outer this fields
                List<Field> newFields = new ArrayList<>();
                for (Field field : clazz.fields) {
                    if (outerThisFields.get(field.owner.getFullName() + "." + field.name) == null) {
                        newFields.add(field);
                    }
                }
                clazz.fields = newFields;
            }

            @Override
            public void visit(Method method) {
                if (method.name.equals("<init>") &&
                    !method.parameters.isEmpty() &&
                    outerThisParams.get(method.parameters.get(0)) != null) {
                    method.parameters.remove(0);
                }

                super.visit(method);
            }
        }.visit(clazz);
    }
}
