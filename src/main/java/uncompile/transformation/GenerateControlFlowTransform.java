package uncompile.transformation;

import uncompile.DecompilationNotPossibleException;
import uncompile.DecompilationSettings;
import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.*;

public class GenerateControlFlowTransform implements Transformation {
    private static class ControlFlowBlock {
        public Block contents = new Block();
        public Set<ControlFlowBlock> previous = new HashSet<>();
        public ControlFlowBlock next = null;
        public Expression condition = null;
        public ControlFlowBlock ifTrue = null;
    }

    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(Block block) {
                super.visit(block);
                run(block);
            }
        }.visit(clazz);
    }

    private void run(Block block) {
        block.expressions = transform(buildControlFlowGraph(block)).expressions;
    }

    private ControlFlowBlock buildControlFlowGraph(Block block) {
        ControlFlowBlock startBlock = new ControlFlowBlock();
        ControlFlowBlock currentBlock = startBlock;
        Map<Label, ControlFlowBlock> labelToBlock = new HashMap<>();
        for (Expression expression : block) {
            if (currentBlock == null && !(expression instanceof Label)) if (!DecompilationSettings.IGNORE_UNREACHABLE_CODE) throw new DecompilationNotPossibleException("unreachable code");
            else continue;

            if (expression instanceof Label) {
                ControlFlowBlock newBlock = labelToBlock.computeIfAbsent((Label) expression, k -> new ControlFlowBlock());
                if (currentBlock != null) {
                    currentBlock.next = newBlock;
                    newBlock.previous.add(currentBlock);
                }
                currentBlock = newBlock;
                continue;
            }

            if (expression instanceof Goto) {
                Goto gotoExpr = (Goto) expression;
                ControlFlowBlock gotoBlock = labelToBlock.computeIfAbsent(gotoExpr.target, k -> new ControlFlowBlock());
                if (gotoExpr.condition == null) {
                    currentBlock.next = gotoBlock;
                    gotoBlock.previous.add(currentBlock);
                    currentBlock = null;
                } else {
                    currentBlock.condition = gotoExpr.condition;
                    currentBlock.ifTrue = gotoBlock;
                    gotoBlock.previous.add(currentBlock);
                    ControlFlowBlock next = new ControlFlowBlock();
                    currentBlock.next = next;
                    next.previous.add(currentBlock);
                    currentBlock = next;
                }
                continue;
            }

            currentBlock.contents.add(expression);

            if (expression instanceof Return) currentBlock = null;
        }
        return startBlock;
    }

    private Block transform(ControlFlowBlock startBlock) {
        Block finalResult = new Block();

        Map<ControlFlowBlock, Label> loops = new HashMap<>();
        int loopCounter = 0;
        Deque<ControlFlowBlock> sourceStack = new ArrayDeque<>();
        Deque<Block> blockStack = new ArrayDeque<>();
        Deque<Optional<ControlFlowBlock>> commonDescendantStack = new ArrayDeque<>();
        Set<ControlFlowBlock> visited = new HashSet<>();
        sourceStack.push(startBlock);
        blockStack.push(finalResult);
        commonDescendantStack.push(Optional.empty());
        while (!sourceStack.isEmpty()) {
            ControlFlowBlock source = sourceStack.pop();
            Block block = blockStack.pop();
            Optional<ControlFlowBlock> expectedCommonDescendant = commonDescendantStack.pop();
            if (visited.contains(source)) continue;
            visited.add(source);

            if (!source.previous.isEmpty()) {
                Label loopLabel = new Label("loop" + loopCounter++);
                loops.put(source, loopLabel);
                WhileLoop loop = new WhileLoop(new BooleanLiteral(true), new Block());
                block.add(loopLabel);
                block.add(loop);
                Block beforeBreak = new Block();
                loop.body.add(beforeBreak);
                loop.body.add(new Break(loopLabel));
                block = beforeBreak;
            }

            block.expressions.addAll(source.contents.expressions);

            if (!block.expressions.isEmpty() && (source.next == null) != block.expressions.get(block.expressions.size() - 1) instanceof Return) throw new AssertionError("Bad control flow graph");

            if (source.next == null) continue;

            Label nextLoop = loops.get(source.next);

            if (source.condition != null) {
                Label loop = loops.get(source.ifTrue);
                if (loop != null) {
                    If ifExpr = new If(source.condition, new Block(), null);
                    ifExpr.ifBlock.add(new Continue(loop));
                    block.add(ifExpr);
                } else if (nextLoop != null) {
                    If ifExpr = new If(new UnaryOperation(UnaryOperator.NOT, new Par(source.condition)), new Block(), null);
                    ifExpr.ifBlock.add(new Continue(nextLoop));
                    block.add(ifExpr);
                    sourceStack.push(source.ifTrue);
                    blockStack.push(block);
                    commonDescendantStack.push(expectedCommonDescendant);
                    continue;
                } else {
                    If ifExpr = new If(source.condition, new Block(), new Block());
                    block.add(ifExpr);
                    ControlFlowBlock commonDescendant = getCommonDescendant(source.next, source.ifTrue);
                    sourceStack.push(source.ifTrue);
                    blockStack.push(ifExpr.ifBlock);
                    commonDescendantStack.push(Optional.ofNullable(commonDescendant));
                    sourceStack.push(source.next);
                    blockStack.push(ifExpr.elseBlock);
                    commonDescendantStack.push(Optional.ofNullable(commonDescendant));
                    if (commonDescendant != null) {
                        // If there is more than one common descendant, then no equivalent Java code exists
                        // (without duplicating blocks), and a 'continue' statement pointing to a label not
                        // on an enclosing loop will be added. The code won't recompile, but the reader can
                        // interpret this as a 'goto' statement.
                        sourceStack.push(commonDescendant);
                        blockStack.push(block); // visit before visiting branches
                        commonDescendantStack.push(expectedCommonDescendant);
                    }
                    continue;
                }
            }

            if (nextLoop != null) {
                if (expectedCommonDescendant.orElse(null) != source.next) {
                    block.expressions.add(new Continue(nextLoop));
                }
                continue;
            }

            sourceStack.push(source.next);
            blockStack.push(block);
            commonDescendantStack.push(expectedCommonDescendant);
        }

        return finalResult;
    }

    private ControlFlowBlock getCommonDescendant(ControlFlowBlock a, ControlFlowBlock b) {
        Set<ControlFlowBlock> descendantsOfA = new HashSet<>();
        Set<ControlFlowBlock> descendantsOfB = new HashSet<>();
        Set<ControlFlowBlock> allDescendants = new HashSet<>();
        Deque<ControlFlowBlock> nextDescendantsOfA = new ArrayDeque<>();
        Deque<ControlFlowBlock> nextDescendantsOfB = new ArrayDeque<>();
        nextDescendantsOfA.addLast(a);
        nextDescendantsOfB.addLast(b);
        while (!nextDescendantsOfA.isEmpty() || !nextDescendantsOfB.isEmpty()) {
            if (!nextDescendantsOfA.isEmpty()) {
                ControlFlowBlock descendant = nextDescendantsOfA.removeFirst();
                if (descendantsOfA.add(descendant)) {
                    if (!allDescendants.add(descendant)) return descendant;

                    if (descendant.next != null) nextDescendantsOfA.addLast(descendant.next);
                    if (descendant.ifTrue != null) nextDescendantsOfA.addLast(descendant.ifTrue);
                }
            }

            if (!nextDescendantsOfB.isEmpty()) {
                ControlFlowBlock descendant = nextDescendantsOfB.removeFirst();
                if (descendantsOfB.add(descendant)) {
                    if (!allDescendants.add(descendant)) return descendant;

                    if (descendant.next != null) nextDescendantsOfB.addLast(descendant.next);
                    if (descendant.ifTrue != null) nextDescendantsOfB.addLast(descendant.ifTrue);
                }
            }
        }

        return null;
    }
}
