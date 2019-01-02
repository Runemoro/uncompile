package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * - Moves variable declarations to right before their first usage
 * - Moves variable declarations into the innermost block they're used
 * - Merges variable declarations with their assignment
 * - Removes unused variable declarations if there are any
 */
public class BringVariableDeclarationsCloserTransform implements Transformation {
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
        // TODO: optimize getVariables by caching results
        new AstVisitor() {
            private Set<VariableDeclaration> removedDeclarations = new HashSet<>();

            @Override
            public void visit(Block block) {
                // Remove all declarations
                List<Expression> newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    if (expression instanceof VariableDeclaration) {
                        removedDeclarations.add((VariableDeclaration) expression);
                        continue;
                    }

                    newExpressions.add(expression);
                }
                block.expressions = newExpressions;

                // Check which declarations can be moved to an inner block
                Set<VariableDeclaration> variablesInInnerBlock = new HashSet<>();
                Set<VariableDeclaration> movableToInner = new HashSet<>(removedDeclarations);

                for (Expression expression : block) {
                    for (VariableDeclaration variable : getVariables(expression)) {
                        if (!movableToInner.contains(variable)) {
                            continue;
                        }

                        // Check that another block doesn't already contain the variable
                        if (variablesInInnerBlock.contains(variable)) {
                            movableToInner.remove(variable);
                            continue;
                        }

                        // Get the inner blocks of the expression
                        Block[] innerBlocks;
                        if (expression instanceof If) {
                            If ifExpr = (If) expression;

                            // If the if condition contains the variable, then the variable is not movable
                            if (getVariables(ifExpr.condition).contains(variable)) {
                                movableToInner.remove(variable);
                                continue;
                            }

                            innerBlocks = new Block[]{ifExpr.ifBlock, ifExpr.elseBlock};
                        } else if (expression instanceof WhileLoop) {
                            // This is safe to do assuming the input code is valid Java:
                            // If the variable was meant to be shared across loop iterations,
                            // then it must necessarily have been initialized outside of the
                            // loop to avoid a "variable used before assignment" error.
                            innerBlocks = new Block[]{((WhileLoop) expression).body};
                        } else if (expression instanceof Block) {
                            innerBlocks = new Block[]{(Block) expression};
                        } else {
                            // Expression doesn't have inner blocks, so variable isn't movable
                            movableToInner.remove(variable);
                            continue;
                        }

                        for (Block innerBlock : innerBlocks) {
                            if (innerBlock != null && getVariables(innerBlock).contains(variable)) {
                                if (variablesInInnerBlock.contains(variable)) {
                                    // Variable also in another inner block
                                    movableToInner.remove(variable);
                                    break;
                                }

                                variablesInInnerBlock.add(variable);
                            }
                        }
                    }
                }

                // Place unmovable variables right before their first usage and merge declarations with assignments
                newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    for (VariableDeclaration variable : getVariables(expression)) {
                        if (removedDeclarations.contains(variable) && !movableToInner.contains(variable)) {
                            if (expression instanceof Assignment) {
                                ((Assignment) expression).left = variable;
                            } else {
                                newExpressions.add(variable);
                            }
                            removedDeclarations.remove(variable);
                        }
                    }

                    newExpressions.add(expression);
                }

                block.expressions = newExpressions;

                // Visiting subblocks must be done after removedDeclarations is updated
                super.visit(block);
            }
        }.visit(method.body);
    }

    private static Set<VariableDeclaration> getVariables(Expression expression) {
        Set<VariableDeclaration> variables = new HashSet<>();

        new AstVisitor() {
            @Override
            public void visit(VariableReference variableReference) {
                super.visit(variableReference);
                variables.add(variableReference.declaration);
            }
        }.visit(expression);

        return variables;
    }
}
