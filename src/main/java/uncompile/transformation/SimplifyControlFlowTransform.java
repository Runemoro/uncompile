package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.*;

/**
 * - Removes unnecessary blocks
 * - Removes unreachable statements that follow continue, break, or return
 * - Removes unnecessary continue, break or return statements
 * - Removes unnecessary labels for continue, break or return statements
 * <p>
 * Assumes there are only while loops and all loop conditions are 'true'.
 */
public class SimplifyControlFlowTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                super.visit(method);
                if (method.body != null) {
                    run(method);
                }
            }
        }.visit(clazz);
    }

    private void run(Method method) {
        // Remove unnecessary blocks
        removeUnnecessaryBlocks(method);

        // Simplify loops that don't loop and remove unreachable statements
        while (simplifyLoops(method)) {}
    }

    private void removeUnnecessaryBlocks(Method method) {
        new AstVisitor() {
            @Override
            public void visit(Block block) {
                super.visit(block);

                List<Expression> newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    if (expression instanceof Block) {
                        for (Expression innerExpression : (Block) expression) {
                            newExpressions.add(innerExpression);
                        }
                        continue;
                    }

                    newExpressions.add(expression);
                }
                block.expressions = newExpressions;
            }
        }.visit(method.body);
    }

    private boolean simplifyLoops(Method method) {
        boolean[] needsAnotherPass = {false};
        Set<WhileLoop> unsimplifiableLoops = new HashSet<>();
        new AstVisitor() {
            private boolean dontUnsetCurrentLoop = false;
            private WhileLoop currentLoop = null;
            private WhileLoop closestOuterLoop = null;
            private Map<Label, WhileLoop> labelToLoop = new HashMap<>();

            @Override
            public void visit(Block block) {
                WhileLoop oldLoop = currentLoop;
                if (!dontUnsetCurrentLoop) {
                    currentLoop = null;
                }
                dontUnsetCurrentLoop = false;

                Label lastLabel = null;
                List<Expression> newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    if (expression instanceof WhileLoop) {
                        WhileLoop whileLoop = (WhileLoop) expression;

                        if (lastLabel != null) {
                            labelToLoop.put(lastLabel, whileLoop);
                        }

                        WhileLoop oldClosestOuterLoop = closestOuterLoop;
                        closestOuterLoop = whileLoop;
                        visit(whileLoop);
                        closestOuterLoop = oldClosestOuterLoop;
                        newExpressions.add(expression);
                        continue;
                    }

                    if (expression instanceof Continue) {
                        Continue continueExpr = (Continue) expression;
                        WhileLoop loop = continueExpr.label == null ? closestOuterLoop : labelToLoop.get(continueExpr.label);
                        if (loop == null) { // caused by control flow with no equivalent java code (see comment in GenerateControlFlowTransform)
                            newExpressions.add(expression);
                            break;
                        }
                        if (loop == closestOuterLoop && currentLoop == null) {
                            unsimplifiableLoops.add(closestOuterLoop);
                        }

                        if (currentLoop == null || loop != closestOuterLoop) {
                            newExpressions.add(expression);
                            unsimplifiableLoops.add(loop);
                        }

                        break;
                    }

                    newExpressions.add(expression);

                    if (expression instanceof Return) {
                        break;
                    }

                    if (expression instanceof Break) {
                        Break breakExpr = (Break) expression;
                        WhileLoop loop = breakExpr.label == null ? closestOuterLoop : labelToLoop.get(breakExpr.label);
                        if (loop == null) {
                            throw new AssertionError();
                        }
                        if (loop == closestOuterLoop && currentLoop == null) {
                            unsimplifiableLoops.add(closestOuterLoop);
                        }

                        if (loop == closestOuterLoop) {
                            breakExpr.label = null; // necessary for the next visitor to work properly
                        }
                        break;
                    }

                    visit(expression);

                    lastLabel = expression instanceof Label ? (Label) expression : null;
                }

                if (newExpressions.size() != block.expressions.size()) {
                    needsAnotherPass[0] = true;
                }

                block.expressions = newExpressions;

                currentLoop = oldLoop;
            }

            @Override
            public void visit(WhileLoop whileLoop) {
                WhileLoop oldCurrentLoop = currentLoop;
                currentLoop = whileLoop;
                dontUnsetCurrentLoop = true;
                super.visit(whileLoop);
                currentLoop = oldCurrentLoop;
            }
        }.visit(method.body);

        new TransformingAstVisitor() {
            @Override
            public Expression transform(Expression expression) {
                if (expression instanceof WhileLoop) {
                    WhileLoop loop = (WhileLoop) expression;

                    if (loop.body.expressions.isEmpty()) {
                        return expression;
                    }

                    Expression lastExpression = loop.body.expressions.get(loop.body.expressions.size() - 1);
                    if (!unsimplifiableLoops.contains(loop) && (
                            lastExpression instanceof Break ||
                            lastExpression instanceof Continue || // to an outer loop, because it wasn't removed by last visitor
                            lastExpression instanceof Return)) {
                        if (lastExpression instanceof Break && ((Break) lastExpression).label == null) {
                            loop.body.expressions.remove(loop.body.expressions.size() - 1);
                        }
                        needsAnotherPass[0] = true;
                        return ((WhileLoop) expression).body;
                    }
                }

                return expression;
            }
        }.visit(method.body);

        removeUnnecessaryBlocks(method);

        return needsAnotherPass[0];
    }
}
