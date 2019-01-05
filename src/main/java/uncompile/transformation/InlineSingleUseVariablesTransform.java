package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Inlines single-use variables that are used immediately after assignment.
 * Variables known to have been in the original code (if they were present
 * in the LVT) are not inlined.
 * <p>
 * Depends on BringVariableDeclarationsCloserTransform. Should run after
 * RemoveUnusedAssignmentsTransform for best results.
 */
public class InlineSingleUseVariablesTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                if (method.body != null) {
                    run(method);
                }
            }
        }.visit(clazz);
    }

    private void run(Method method) {

        // Get single-use variables
        Set<VariableDeclaration> variables = new HashSet<>();
        Set<VariableDeclaration> variablesUsedTwice = new HashSet<>();
        Set<VariableDeclaration> variablesAssignedOnce = new HashSet<>();

        new AstVisitor() {
            @Override
            public void visit(VariableReference variableReference) {
                super.visit(variableReference);

                VariableDeclaration variable = variableReference.declaration;
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
        }.visit((Expression) method.body);

        variables.removeAll(variablesUsedTwice);

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
                List<Expression> newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    if (pendingAssignment != null) {

                        boolean inlined = inline(expression, toInline, pendingAssignment.right);
                        changed[0] |= inlined;
                        if (!inlined) {
                            newExpressions.add(pendingAssignment);
                        }

                        pendingAssignment = null;
                        toInline = null;
                    }

                    if (expression instanceof Assignment) {
                        Assignment assignment = (Assignment) expression;
                        if (assignment.left instanceof VariableReference) {
                            VariableReference variable = (VariableReference) assignment.left;
                            if (variables.contains(variable.declaration)) {
                                pendingAssignment = assignment;
                                toInline = variable.declaration;
                            }
                        }
                    }

                    if (pendingAssignment == null) {
                        newExpressions.add(expression);
                    }
                }

                block.expressions = newExpressions;

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
    private boolean inline(Expression expression, VariableDeclaration variable, Expression value) {
        boolean[] changed = {false};
        new TransformingAstVisitor() {
            private boolean canSafelyInline = true;

            @Override
            public void afterVisit(AstNode node) {
                if (!(node instanceof Cast ||
                      node instanceof VariableReference ||
                      node instanceof BooleanLiteral ||
                      node instanceof CharLiteral ||
                      node instanceof IntLiteral ||
                      node instanceof LongLiteral ||
                      node instanceof DoubleLiteral ||
                      node instanceof ClassLiteral ||
                      node instanceof TypeNode ||
                      node instanceof ClassReference ||
                      node instanceof StringLiteral)) {
                    canSafelyInline = false;
                }
            }

            @Override
            public Expression transform(Expression expression) {
                if (canSafelyInline && expression instanceof VariableReference && ((VariableReference) expression).declaration == variable) {
                    canSafelyInline = false;
                    changed[0] = true;
                    return value;
                }
                return expression;
            }
        }.visit(expression);

        return changed[0];
    }
}
