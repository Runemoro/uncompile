package uncompile.transformation;

import uncompile.DecompilationNotPossibleException;
import uncompile.DecompilationSettings;
import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.*;

public class GenerateControlFlowTransform implements Transformation {
    private static class ControlFlowBlock {
        public Block contents = new Block();
        public Set<ControlFlowBlock> incomingBlocks = new LinkedHashSet<>();
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
        ControlFlowBlock controlFlowGraph = buildControlFlowGraph(block);
        block.expressions = transform(controlFlowGraph).expressions;
    }

    private ControlFlowBlock buildControlFlowGraph(Block block) {
        ControlFlowBlock startBlock = new ControlFlowBlock();
        ControlFlowBlock currentBlock = startBlock;
        Map<Label, ControlFlowBlock> labelToBlock = new HashMap<>();
        List<Switch> switches = new ArrayList<>();
        for (Expression expression : block) {
            if (currentBlock == null && !(expression instanceof Label)) {
                if (!DecompilationSettings.IGNORE_UNREACHABLE_CODE) {
                    throw new DecompilationNotPossibleException("unreachable code");
                } else {
                    continue;
                }
            }

            if (expression instanceof Label) {
                ControlFlowBlock newBlock = labelToBlock.computeIfAbsent((Label) expression, k -> new ControlFlowBlock());
                if (currentBlock != null) {
                    currentBlock.next = newBlock;
                    newBlock.incomingBlocks.add(currentBlock);
                }
                currentBlock = newBlock;
                continue;
            }

            if (expression instanceof Goto) {
                Goto gotoExpr = (Goto) expression;
                ControlFlowBlock gotoBlock = labelToBlock.computeIfAbsent(gotoExpr.target, k -> new ControlFlowBlock());
                if (gotoExpr.condition == null) {
                    currentBlock.next = gotoBlock;
                    gotoBlock.incomingBlocks.add(currentBlock);
                    currentBlock = null;
                } else {
                    currentBlock.condition = gotoExpr.condition;
                    currentBlock.ifTrue = gotoBlock;
                    gotoBlock.incomingBlocks.add(currentBlock);
                    ControlFlowBlock next = new ControlFlowBlock();
                    currentBlock.next = next;
                    next.incomingBlocks.add(currentBlock);
                    if (DecompilationSettings.FLIP_JUMP_CONDITIONS) {
                        flipBlock(currentBlock);
                    }
                    currentBlock = next;
                }
                continue;
            }

            currentBlock.contents.add(expression);

            if (expression instanceof Switch) {
                switches.add((Switch) expression);
                currentBlock = null;
            }

            if (expression instanceof Return) {
                currentBlock = null;
            }
        }

        return startBlock;
    }

    private void flipBlock(ControlFlowBlock currentBlock) {
        ControlFlowBlock ifTrue = currentBlock.ifTrue;
        currentBlock.ifTrue = currentBlock.next;
        currentBlock.next = ifTrue;
        currentBlock.condition = AstUtil.negate(currentBlock.condition);
    }

    private Block transform(ControlFlowBlock startBlock) {
        Block result = new Block();
        Map<ControlFlowBlock, Label> loops = new HashMap<>();
        int loopCounter = 0;
        Deque<ControlFlowBlock> sourceStack = new ArrayDeque<>();
        Deque<Block> blockStack = new ArrayDeque<>();
        Deque<Optional<ControlFlowBlock>> commonDescendantStack = new ArrayDeque<>();
        Set<ControlFlowBlock> visited = new HashSet<>();
        sourceStack.push(startBlock);
        blockStack.push(result);
        commonDescendantStack.push(Optional.empty());

        while (!sourceStack.isEmpty()) {
            ControlFlowBlock source = sourceStack.pop();
            Block block = blockStack.pop();
            Optional<ControlFlowBlock> expectedCommonDescendant = commonDescendantStack.pop();
            if (!visited.add(source)) {
                continue;
            }

            if (source.incomingBlocks.size() > 1) {
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

            if ((source.next == null) != (!block.expressions.isEmpty() && (
                    block.expressions.get(block.expressions.size() - 1) instanceof Return ||
                    block.expressions.get(block.expressions.size() - 1) instanceof Switch))) {
                throw new AssertionError("Bad control flow graph");
            }

            if (source.next == null) {
                continue;
            }

            if (source.ifTrue != null && source.ifTrue == expectedCommonDescendant.orElse(null)) {
                flipBlock(source);
            }

            if (source.next == expectedCommonDescendant.orElse(null)) {
                if (source.ifTrue != null) {
                    If ifExpr = new If(source.condition, new Block(), null);
                    block.add(ifExpr);
                    sourceStack.push(source.ifTrue);
                    blockStack.push(ifExpr.ifBlock);
                    commonDescendantStack.push(expectedCommonDescendant);
                }
                continue;
            }

            Label nextLoop = loops.get(source.next);

            if (source.condition != null) {
                Label loop = loops.get(source.ifTrue);
                if (loop != null) {
                    If ifExpr = new If(source.condition, new Block(), null);
                    ifExpr.ifBlock.add(new Continue(loop));
                    block.add(ifExpr);
                } else if (nextLoop != null) {
                    If ifExpr = new If(AstUtil.negate(source.condition), new Block(), null);
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

        return result;
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
                    if (!allDescendants.add(descendant)) {
                        return descendant;
                    }

                    if (descendant.next != null) {
                        nextDescendantsOfA.addLast(descendant.next);
                    }
                    if (descendant.ifTrue != null) {
                        nextDescendantsOfA.addLast(descendant.ifTrue);
                    }
                }
            }

            if (!nextDescendantsOfB.isEmpty()) {
                ControlFlowBlock descendant = nextDescendantsOfB.removeFirst();
                if (descendantsOfB.add(descendant)) {
                    if (!allDescendants.add(descendant)) {
                        return descendant;
                    }

                    if (descendant.next != null) {
                        nextDescendantsOfB.addLast(descendant.next);
                    }
                    if (descendant.ifTrue != null) {
                        nextDescendantsOfB.addLast(descendant.ifTrue);
                    }
                }
            }
        }

        return null;
    }
}
