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
 * <p>
 * Should run after RemoveUnusedAssignmentsTransform.
 */
public class BringVariableDeclarationsCloserTransformation implements Transformation {
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
        // TODO: optimize getVariables by caching results
        new AstVisitor() {
            private Set<VariableDeclaration> removedDeclarations = new HashSet<>();

            @Override
            public void visit(Block block) {
                // Remove all declarations
                List<Statement> newStatements = new ArrayList<>();
                for (Statement statement : block) {
                    if (statement instanceof ExpressionStatement && ((ExpressionStatement) statement).expression instanceof VariableDeclaration) {
                        removedDeclarations.add((VariableDeclaration) ((ExpressionStatement) statement).expression);
                        continue;
                    }

                    newStatements.add(statement);
                }
                block.statements = newStatements;

                // Check which declarations can be moved to an inner block
                Set<VariableDeclaration> variablesInInnerBlock = new HashSet<>();
                Set<VariableDeclaration> movableToInner = new HashSet<>(removedDeclarations);

                for (Statement statement : block) {
                    for (VariableDeclaration variable : getVariables(statement)) {
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
                        if (statement instanceof If) {
                            If ifExpr = (If) statement;

                            // If the if condition contains the variable, then the variable is not movable
                            if (getVariables(ifExpr.condition).contains(variable)) {
                                movableToInner.remove(variable);
                                continue;
                            }

                            innerBlocks = new Block[]{ifExpr.ifBlock, ifExpr.elseBlock};
                        } else if (statement instanceof WhileLoop) {
                            // This is safe to do assuming the input code is valid Java:
                            // If the variable was meant to be shared across loop iterations,
                            // then it must necessarily have been initialized outside of the
                            // loop to avoid a "variable used before assignment" error.
                            innerBlocks = new Block[]{((WhileLoop) statement).body};
                        } else if (statement instanceof Block) {
                            innerBlocks = new Block[]{(Block) statement};
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
                newStatements = new ArrayList<>();
                for (Statement statement : block) {
                    for (VariableDeclaration variable : getVariables(statement)) {
                        if (removedDeclarations.contains(variable) && !movableToInner.contains(variable)) {
                            if (statement instanceof ExpressionStatement &&
                                ((ExpressionStatement) statement).expression instanceof Assignment &&
                                ((Assignment) ((ExpressionStatement) statement).expression).left instanceof VariableReference &&
                                ((VariableReference) ((Assignment) ((ExpressionStatement) statement).expression).left).declaration == variable) {
                                ((Assignment) ((ExpressionStatement) statement).expression).left = variable;
                            } else {
                                newStatements.add(new ExpressionStatement(variable));
                            }
                            removedDeclarations.remove(variable);
                        }
                    }

                    newStatements.add(statement);
                }

                block.statements = newStatements;

                // Visiting subblocks must be done after removedDeclarations is updated
                super.visit(block);
            }
        }.visit(method.body);
    }

    private static Set<VariableDeclaration> getVariables(AstNode expression) {
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
