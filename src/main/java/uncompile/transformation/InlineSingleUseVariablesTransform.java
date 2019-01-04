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
 * Depends on BringVariableDeclarationsCloserTransform.
 * Must run before any other transformation that inlines method calls (and must
 * not be run twice) to avoid method call order from being changed
 */
public class InlineSingleUseVariablesTransform implements Transformation { // TODO: all assignments, possibly remove inline alias
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

        // Inline single-use variables used immediately after their first assignment
        new AstVisitor() {
            @Override
            public void visit(Block block) {
                Assignment pendingAssignment = null;
                VariableDeclaration toInline = null;
                List<Expression> newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    if (pendingAssignment != null) {
                        boolean[] replaced = {false};

                        VariableDeclaration toReplace = toInline;
                        Expression replaceWith = pendingAssignment.right;
                        new TransformingAstVisitor() {
                            @Override
                            public Expression transform(Expression expression) {
                                if (expression instanceof VariableReference && ((VariableReference) expression).declaration.equals(toReplace)) {
                                    replaced[0] = true;
                                    return replaceWith;
                                }

                                return expression;
                            }
                        }.visit(expression);

                        if (!replaced[0]) {
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
    }
}
