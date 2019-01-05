package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;
import uncompile.util.Util;

import java.util.*;

public class InlineAliasVariablesTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                if (method.body != null) {
                    while (run(method)) {};
                    // TODO: change loop logic so that it can be done in a single pass
                }
            }
        }.visit(clazz);
    }

    private boolean run(Method method) {
        Map<Expression, Optional<Expression>> substitutions = new HashMap<>();

        new AstVisitor() {
            Map<VariableDeclaration, Set<VariableDeclaration>> dependants = new HashMap<>();
            Map<VariableDeclaration, VariableDeclaration> values = new HashMap<>();

            @Override
            public void visit(Assignment assignment) {
                visit(assignment.right);
                Expression transformedRight = substitutions.getOrDefault(assignment.right, Optional.empty()).orElse(assignment.right);

                if (assignment.left instanceof VariableReference) {
                    VariableDeclaration variable = ((VariableReference) assignment.left).declaration;

                    if (transformedRight instanceof VariableReference && ((VariableReference) transformedRight).declaration == variable) {
                        substitutions.put(assignment, Optional.empty());
                        return;
                    }

                    for (VariableDeclaration dependant : dependants.getOrDefault(variable, Collections.emptySet())) {
                        values.remove(dependant);
                    }
                    dependants.remove(variable);

                    if (assignment.right instanceof VariableReference) {
                        VariableDeclaration rightVariable = ((VariableReference) assignment.right).declaration;
                        values.put(variable, values.getOrDefault(rightVariable, rightVariable));
                        dependants.computeIfAbsent(rightVariable, k -> new HashSet<>()).add(variable);
                    }

                    // TODO: constants too?
                } else {
                    visit(assignment.left);
                }
            }

            @Override
            public void visit(If ifExpr) {
                visit(ifExpr.condition);
                Map<VariableDeclaration, Set<VariableDeclaration>> oldDependants = dependants;
                Map<VariableDeclaration, VariableDeclaration> oldValues = values;

                dependants = Util.deepCopy(oldDependants);
                values = new HashMap<>(oldValues);
                visit(ifExpr.ifBlock);
                Map<VariableDeclaration, VariableDeclaration> values1 = values;

                Map<VariableDeclaration, VariableDeclaration> values2;
                if (ifExpr.elseBlock != null) {
                    dependants = Util.deepCopy(oldDependants);
                    values = new HashMap<>(oldValues);
                    visit(ifExpr.elseBlock);
                    values2 = values;
                } else {
                    values2 = oldValues;
                }

                values = new HashMap<>();
                for (Map.Entry<VariableDeclaration, VariableDeclaration> entry : Util.union(values1.entrySet(), values2.entrySet())) {
                    if (values1.get(entry.getKey()) == values2.get(entry.getKey())) {
                        values.put(entry.getKey(), entry.getValue());
                    }
                }

                recalculateDependants();
            }

            @Override
            public void visit(WhileLoop whileLoop) {
                visit(whileLoop.condition);

                Set<VariableDeclaration> changedVariables = new HashSet<>();
                new AstVisitor() {
                    @Override
                    public void visit(Assignment assignment) {
                        if (assignment.left instanceof VariableReference) {
                            changedVariables.add(((VariableReference) assignment.left).declaration);
                        }
                    }
                }.visit(whileLoop.body);

                for (VariableDeclaration changedVariable : changedVariables) {
                    values.remove(changedVariable);
                }
                recalculateDependants();


                Map<VariableDeclaration, VariableDeclaration> oldValues = values;
                Map<VariableDeclaration, Set<VariableDeclaration>> oldDependants = dependants;

                values = new HashMap<>(oldValues);
                dependants = Util.deepCopy(oldDependants);

                visit(whileLoop.body);

                values = oldValues;
                dependants = oldDependants;
            }

            private void recalculateDependants() {
                dependants = new HashMap<>();
                for (Map.Entry<VariableDeclaration, VariableDeclaration> entry : values.entrySet()) {
                    dependants.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
                }
            }

            @Override
            public void visit(VariableReference variableReference) {
                VariableDeclaration value = values.get(variableReference.declaration);
                if (value != null) {
                    substitutions.put(variableReference, Optional.of(new VariableReference(value)));
                }
            }
        }.visit(method.body);

        new TransformingAstVisitor() {
            @Override
            public Expression transform(Expression expression) {
                return substitutions.getOrDefault(expression, Optional.of(expression)).orElse(null);
            }
        }.visit(method.body);

        return !substitutions.isEmpty();
    }
}
