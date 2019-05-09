package uncompile.transformation;

import uncompile.DecompilationNotPossibleException;
import uncompile.ast.Class;
import uncompile.ast.*;
import uncompile.metadata.ClassType;
import uncompile.metadata.ReferenceType;

import java.util.*;

public class FixInnerClassesTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
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
        }.visit(node);

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

                    ThisReference outerThisReference = outerThisParams.get(variable);
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
        }.visit(node);

        // Replace synthetic fields and methods
        Map<Expression, Optional<Expression>> substitutions = new HashMap<>();
        new AstVisitor() {
            @Override
            public void visit(InstanceFieldReference instanceFieldReference) {
                String qualifiedFieldName = ((ReferenceType) instanceFieldReference.target.getType()).getRawType().getFullName() + "." + instanceFieldReference.field;
                ThisReference outerThisReference = outerThisFields.get(qualifiedFieldName);

                if (outerThisReference != null) {
                    substitutions.put(instanceFieldReference, Optional.of(outerThisReference));
                }
            }

            @Override
            public void visit(Assignment assignment) {
                if (assignment.left instanceof VariableReference) {
                    if (outerThisParams.get(((VariableReference) assignment.left).declaration) != null) {
                        substitutions.put(assignment, Optional.empty());
                    }
                } else if (assignment.left instanceof InstanceFieldReference) {
                    InstanceFieldReference fieldReference = (InstanceFieldReference) assignment.left;
                    String qualifiedFieldName = ((ReferenceType) fieldReference.target.getType()).getRawType().getFullName() + "." + fieldReference.field;
                    ThisReference outerThisReference = outerThisFields.get(qualifiedFieldName);

                    if (outerThisReference != null) {
                        substitutions.put(assignment, Optional.empty());
                    }
                }
            }

            @Override
            public void visit(Class clazz) {
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
        }.visit(node);

        AstUtil.substitute(node, substitutions);
    }
}
