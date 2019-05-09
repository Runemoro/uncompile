package uncompile.transformation;

import uncompile.DecompilationSettings;
import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.*;

/**
 * Inlines single-use variables that are used immediately after assignment.
 * Variables known to have been in the original code (if they were present
 * in the LVT) are not inlined.
 */
public class InlineSingleUseVariablesTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                if (method.body != null) {
                    run(method);
                }
            }
        }.visit(node);
    }

    private void run(Method method) {
        // Get single-use variables
        Set<VariableDeclaration> variables = new HashSet<>();
        Set<VariableDeclaration> variablesUsedTwice = new HashSet<>();
        Set<VariableDeclaration> variablesAssignedOnce = new HashSet<>();
        Set<VariableDeclaration> excludedVariables = new HashSet<>();

        new AstVisitor() {
            @Override
            public void visit(VariableReference variableReference) {
                super.visit(variableReference);

                VariableDeclaration variable = variableReference.declaration;

                if (!DecompilationSettings.INLINE_NON_SYNTHETICS && variable.isSynthetic) {
                    excludedVariables.add(variable);
                }

                if (!variables.contains(variable)) {
                    variables.add(variable);
                } else {
                    variablesUsedTwice.add(variable);
                }
            }

            @Override
            public void visit(Assignment assignment) {
                if (assignment.left instanceof VariableReference) {
                    VariableDeclaration variable = ((VariableReference) assignment.left).declaration;
                    if (!variablesAssignedOnce.contains(variable)) {
                        variablesAssignedOnce.add(variable);
                    } else {
                        variablesUsedTwice.add(variable);
                    }
                } else {
                    visit(assignment.left);
                }
                visit(assignment.right);
            }
        }.visit((Statement) method.body);

        variables.removeAll(variablesUsedTwice);
        variables.removeAll(excludedVariables);

        while (inlineSingleUseVariables(method, variables)) {}
    }

    private boolean inlineSingleUseVariables(Method method, Set<VariableDeclaration> variables) {
        // Inline single-use variables used immediately after their first assignment
        boolean[] changed = new boolean[1];
        new AstVisitor() {
            @Override
            public void visit(Block block) {
                Assignment pendingAssignment = null;
                VariableDeclaration toInline = null;
                List<Statement> newExpressions = new ArrayList<>();
                for (Statement statement : block) {
                    if (pendingAssignment != null) {

                        boolean inlined = inline(statement, toInline, pendingAssignment.right);
                        changed[0] |= inlined;
                        if (!inlined) {
                            newExpressions.add(new ExpressionStatement(pendingAssignment));
                        }

                        pendingAssignment = null;
                        toInline = null;
                    }

                    if (statement instanceof ExpressionStatement && ((ExpressionStatement) statement).expression instanceof Assignment) {
                        Assignment assignment = (Assignment) ((ExpressionStatement) statement).expression;
                        if (assignment.left instanceof VariableReference) {
                            VariableReference variable = (VariableReference) assignment.left;
                            if (variables.contains(variable.declaration)) {
                                pendingAssignment = assignment;
                                toInline = variable.declaration;
                            }
                        }
                    }

                    if (pendingAssignment == null) {
                        newExpressions.add(statement);
                    }
                }

                block.statements = newExpressions;

                super.visit(block);
            }
        }.visit(method.body);

        return changed[0];
    }

    /**
     * Inlines the value of an effectively final variable into an expression that can contain
     * it at most once. Returns false if the expression does not contain the variable or inlining
     * the variable cannot be done without changing the order subexpressions are evaluated in.
     */
    private boolean inline(Statement statement, VariableDeclaration variable, Expression value) {
        boolean[] changed = {false};
        new ReplacingAstVisitor() {
            private boolean canSafelyInline = true;

            @Override
            public void visit(AstNode node) {
                super.visit(node);

                if (canSafelyInline &&
                    !(node instanceof Cast ||
                      node instanceof VariableReference ||
                      node instanceof BooleanLiteral ||
                      node instanceof CharLiteral ||
                      node instanceof IntLiteral ||
                      node instanceof LongLiteral ||
                      node instanceof DoubleLiteral ||
                      node instanceof ClassLiteral ||
                      node instanceof TypeNode ||
                      node instanceof ClassReference ||
                      node instanceof ThisReference ||
                      node instanceof SuperReference ||
                      node instanceof StringLiteral ||
                      node instanceof ParenthesizedExpression)) {
                    canSafelyInline = false;
                }
            }

            @Override
            public void visit(VariableReference variableReference) {
                if (canSafelyInline && variableReference.declaration == variable) {
                    replace(value);
                    changed[0] = true;
                }
            }
        }.visit(statement);

        return changed[0];
    }
}
