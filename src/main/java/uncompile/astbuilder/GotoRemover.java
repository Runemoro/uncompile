package uncompile.astbuilder;

import uncompile.ast.*;

import java.util.*;

public class GotoRemover {
    private final Block block;

    public GotoRemover(Block block) {
        this.block = block;
    }

    public void run() {
        removeUnusedLabels(block);
        convertGotos(block);
        simplify(block);
        removeUnusedLabels(block);
    }

    private void removeUnusedLabels(Block block) {
        Set<Label> usedLabels = new HashSet<>();
        getUsedLabels(block, usedLabels);
        removeUnusedLabels(block, usedLabels);
    }

    private void getUsedLabels(Block block, Set<Label> usedLabels) {
        for (Expression expression : block.expressions) {
            if (expression instanceof Goto) {
                usedLabels.add(((Goto) expression).target);
            }

            if (expression instanceof Break) {
                Label label = ((Break) expression).label;
                if (label != null) {
                    usedLabels.add(label);
                }
            }

            if (expression instanceof Continue) {
                Label label = ((Continue) expression).label;
                if (label != null) {
                    usedLabels.add(label);
                }
            }

            if (expression instanceof WhileLoop) {
                getUsedLabels(((WhileLoop) expression).body, usedLabels);
            }

            if (expression instanceof If) {
                getUsedLabels(((If) expression).ifBlock, usedLabels);
            }
        }
    }

    private void removeUnusedLabels(Block block, Set<Label> usedLabels) {
        List<Expression> newExpressions = new ArrayList<>();
        for (Expression expression : block.expressions) {
            if (expression instanceof Label && !usedLabels.contains(expression)) {
                continue;
            }

            if (expression instanceof WhileLoop) {
                removeUnusedLabels(((WhileLoop) expression).body, usedLabels);
            }

            if (expression instanceof If) {
                removeUnusedLabels(((If) expression).ifBlock, usedLabels);
            }

            newExpressions.add(expression);
        }

        block.expressions = newExpressions;
    }

    private void convertGotos(Block block) {
        List<Expression> newExpressions = new ArrayList<>();
        List<Expression> currentBlock = newExpressions;

        Deque<List<Expression>> blockStack = new ArrayDeque<>();
        Deque<Optional<Label>> ifLabelStack = new ArrayDeque<>();
        Deque<Optional<Label>> loopLabelStack = new ArrayDeque<>();
        Set<Label> loopLabels = new HashSet<>();
        Map<Label, Integer> ifLabels = new HashMap<>();

        for (Expression expression : block.expressions) {
            if (expression instanceof Label) {
                Label label = (Label) expression;

                // End all open if blocks for that label
                while (ifLabels.getOrDefault(label, 0) > 0) {
                    Label loopLabel = loopLabelStack.pop().orElse(null);

                    // If the block being ended is a loop block, insert a break statement before ending
                    if (loopLabel != null) {
                        currentBlock.add(new Break(loopLabel));
                        loopLabels.remove(loopLabel);
                    }

                    currentBlock = blockStack.pop();

                    if (ifLabelStack.pop().orElse(null) == label) {
                        ifLabels.put(label, ifLabels.get(label) - 1);
                    }
                }

                Block newBlock = new Block();
                currentBlock.add(expression);
                currentBlock.add(new WhileLoop(new BooleanLiteral(true), newBlock));
                blockStack.push(currentBlock);
                currentBlock = newBlock.expressions;

                // Add the label onto the loop label stack so that a break is inserted
                loopLabelStack.push(Optional.of(label));
                ifLabelStack.push(Optional.empty());
                loopLabels.add(label);
            } else if (expression instanceof Goto) {
                Goto gotoExpression = (Goto) expression;
                Label label = gotoExpression.target;
                Expression condition = gotoExpression.condition == null ? new BooleanLiteral(true) : gotoExpression.condition;

                if (loopLabels.contains(label)) {
                    Block ifBlock = new Block();
                    ifBlock.add(new Continue(label));
                    currentBlock.add(new If(condition, ifBlock, null));
                } else {
                    Block ifBlock = new Block();
                    currentBlock.add(new If(new UnaryOperation(UnaryOperator.NOT, new Par(condition)), ifBlock, null));

                    blockStack.push(currentBlock);
                    currentBlock = ifBlock.expressions;
                    loopLabelStack.push(Optional.empty());
                    ifLabelStack.push(Optional.of(label));
                    ifLabels.put(label, ifLabels.getOrDefault(label, 0) + 1);
                }
            } else {
                currentBlock.add(expression);
            }
        }

        while (!loopLabelStack.isEmpty()) {
            Label loopLabel = loopLabelStack.pop().orElse(null);
            if (loopLabel != null) {
                currentBlock.add(new Break(loopLabel));
            }
            currentBlock = blockStack.pop();
        }

        block.expressions = newExpressions;
    }

    private void simplify(Block block) {
        while (simplify(block, null, false)) {}
    }

    private boolean simplify(Block block, Label loopLabel, boolean isLoop) {
        boolean changed = false;
        List<Expression> newExpressions = new ArrayList<>();

        Label lastLabel = null;
        for (Expression expression : block) {
            if (expression instanceof WhileLoop) {
                Block inner = ((WhileLoop) expression).body;

                changed |= simplify(inner, lastLabel, true);

                if (!inner.expressions.isEmpty()) {
                    Expression first = inner.expressions.get(0);
                    if (first instanceof Return || first instanceof Break) {
                        changed = true;
                        newExpressions.add(first);
                        continue;
                    }
                }
            }

            if (expression instanceof If) {
                If ifExpression = (If) expression;
                changed |= simplify(ifExpression.ifBlock, loopLabel, false);

                if (ifExpression.condition instanceof BooleanLiteral && ((BooleanLiteral) ifExpression.condition).value) {
                    for (Expression innerExpression : ifExpression.ifBlock) {
                        newExpressions.add(innerExpression);
                    }

                    changed = true;
                    continue;
                }
            }

            if (expression instanceof Break && loopLabel != null && loopLabel.equals(((Break) expression).label)) {
                ((Break) expression).label = null;
                changed = true;
            }

            if (expression instanceof Continue && loopLabel != null && loopLabel.equals(((Continue) expression).label)) {
                ((Continue) expression).label = null;
                changed = true;
            }

            if (isLoop && expression instanceof Continue && ((Continue) expression).label == null) {
                changed = true;
                break;
            }

            newExpressions.add(expression);

            if (expression instanceof Return || expression instanceof Break || expression instanceof Continue) {
                break;
            }

            lastLabel = expression instanceof Label ? (Label) expression : null;
        }

        block.expressions = newExpressions;
        return changed;
    }
}
