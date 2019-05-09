//package uncompile.transformation;
//
//import uncompile.DecompilationNotPossibleException;
//import uncompile.DecompilationSettings;
//import uncompile.ast.Class;
//import uncompile.ast.*;
//
//import java.util.*;
//
//public class GenerateControlFlowTransform implements Transformation {
//    private static class ControlFlowNode {
//        public Block contents = new Block();
//        public Set<ControlFlowNode> incomingBlocks = new LinkedHashSet<>();
//        public ControlFlowNode next = null;
//        public Expression condition = null;
//        public ControlFlowNode ifTrue = null;
//    }
//
//    @Override
//    public void run(AstNode node) {
//        new AstVisitor() {
//            @Override
//            public void visit(Method method) {
//                super.visit(method);
//                run(method.body);
//            }
//        }.visit(node);
//    }
//
//    private void run(Block block) {
//        ControlFlowNode controlFlowGraph = buildControlFlowGraph(block);
//        block.statements = transform(controlFlowGraph).statements;
//    }
//
//    private ControlFlowNode buildControlFlowGraph(Block block) {
//        ControlFlowNode startNode = new ControlFlowNode();
//        ControlFlowNode currentNode = startNode;
//        Map<String, ControlFlowNode> labelToBlock = new HashMap<>();
//        List<Switch> switches = new ArrayList<>();
//        for (Statement expression : block) {
//            if (currentNode == null && !(expression instanceof Label)) {
//                if (!DecompilationSettings.IGNORE_UNREACHABLE_CODE) {
//                    throw new DecompilationNotPossibleException("unreachable code");
//                } else {
//                    continue;
//                }
//            }
//
//            if (expression instanceof Label) {
//                ControlFlowNode newBlock = labelToBlock.computeIfAbsent((Label) expression, k -> new ControlFlowNode());
//                if (currentNode != null) {
//                    currentNode.next = newBlock;
//                    newBlock.incomingBlocks.add(currentNode);
//                }
//                currentNode = newBlock;
//                continue;
//            }
//
//            if (expression instanceof Goto) {
//                Goto gotoExpr = (Goto) expression;
//                ControlFlowNode gotoBlock = labelToBlock.computeIfAbsent(gotoExpr.target, k -> new ControlFlowNode());
//                if (gotoExpr.condition == null) {
//                    currentNode.next = gotoBlock;
//                    gotoBlock.incomingBlocks.add(currentNode);
//                    currentNode = null;
//                } else {
//                    currentNode.condition = gotoExpr.condition;
//                    currentNode.ifTrue = gotoBlock;
//                    gotoBlock.incomingBlocks.add(currentNode);
//                    ControlFlowNode next = new ControlFlowNode();
//                    currentNode.next = next;
//                    next.incomingBlocks.add(currentNode);
//                    if (DecompilationSettings.FLIP_JUMP_CONDITIONS) {
//                        flipBlock(currentNode);
//                    }
//                    currentNode = next;
//                }
//                continue;
//            }
//
//            currentNode.contents.add(expression);
//
//            if (expression instanceof Switch) {
//                switches.add((Switch) expression);
//                currentNode = null;
//            }
//
//            if (expression instanceof Return) {
//                currentNode = null;
//            }
//        }
//
//        return startNode;
//    }
//
//    private void flipBlock(ControlFlowNode currentBlock) {
//        ControlFlowNode ifTrue = currentBlock.ifTrue;
//        currentBlock.ifTrue = currentBlock.next;
//        currentBlock.next = ifTrue;
//        currentBlock.condition = AstUtil.negate(currentBlock.condition);
//    }
//
//    private Block transform(ControlFlowNode startNode) {
//        Block result = new Block();
//        Map<ControlFlowNode, Label> continues = new HashMap<>();
//        Map<ControlFlowNode, Label> breaks = new HashMap<>();
//        int loopCounter = 0;
//        Deque<ControlFlowNode> nodeStack = new ArrayDeque<>();
//        Deque<Block> blockStack = new ArrayDeque<>();
//        Set<ControlFlowNode> visited = new HashSet<>();
//        nodeStack.push(startNode);
//        blockStack.push(result);
//
//        while (!nodeStack.isEmpty()) {
//            ControlFlowNode node = nodeStack.pop();
//            Block block = blockStack.pop();
//            if (!visited.add(node)) {
//                continue;
//            }
//
//            boolean hasBackwardJumps = false;
//            for (ControlFlowNode incoming : node.incomingBlocks) {
//                if (incoming == node || !visited.contains(incoming)) {
//                    hasBackwardJumps = true;
//                    break;
//                }
//            }
//
//            if (hasBackwardJumps) {
//                Label loopLabel = new Label("loop" + loopCounter++);
//                continues.put(node, loopLabel);
//                WhileLoop loop = new WhileLoop(new BooleanLiteral(true), new Block());
//                block.add(loopLabel);
//                block.add(loop);
//                Block beforeBreak = new Block();
//                loop.body.add(beforeBreak);
//                loop.body.add(new Break(loopLabel));
//                block = beforeBreak;
//            }
//
//            block.statements.addAll(node.contents.statements);
//
//            if ((node.next == null) != (!block.statements.isEmpty() && (
//                    block.statements.get(block.statements.size() - 1) instanceof Return ||
//                    block.statements.get(block.statements.size() - 1) instanceof Switch))) {
//                throw new AssertionError("Bad control flow graph");
//            }
//
//            if (node.next == null) {
//                continue;
//            }
//
//            Label nextContinue = continues.get(node.next);
//            Label nextBreak = breaks.get(node.next);
//
//            if (node.condition != null) {
//                Label ifContinue = continues.get(node.ifTrue);
//                Label ifBreak = breaks.get(node.ifTrue);
//
//                if (ifBreak == null && nextBreak != null || ifContinue == null && ifBreak == null && (nextBreak != null || nextContinue != null)) {
//                    flipBlock(node);
//                    Label oldIfContinue = ifContinue;
//                    ifContinue = nextContinue;
//                    nextContinue = oldIfContinue;
//                    Label oldIfBreak = ifBreak;
//                    ifBreak = nextBreak;
//                    nextBreak = oldIfBreak;
//                }
//
//                if (ifBreak != null) {
//                    If ifExpr = new If(node.condition, new Block(), null);
//                    ifExpr.ifBlock.add(new Break(ifBreak));
//                    block.add(ifExpr);
//                } else if (ifContinue != null) {
//                    If ifExpr = new If(node.condition, new Block(), null);
//                    ifExpr.ifBlock.add(new Continue(ifContinue));
//                    block.add(ifExpr);
//                } else {
//                    // loop1: <-- breaks.get(commonDescendant)
//                    // while (true) {
//                    //     loop2: <-- breaks.get(next)
//                    //     while (true) {
//                    //         if (condition) {
//                    //             (ifTrue branch)
//                    //         }
//                    //         break;
//                    //     }
//                    //     (next branch)
//                    // }
//
//                    ControlFlowNode commonDescendant = getCommonDescendant(node.next, node.ifTrue, new HashSet<>());
//
//                    Block inside = block;
//
//                    if (commonDescendant != null && !continues.containsKey(commonDescendant) && !breaks.containsKey(commonDescendant)) {
//                        Label label1 = new Label("loop" + loopCounter++);
//                        WhileLoop loop1 = new WhileLoop(new BooleanLiteral(true), new Block());
//                        block.add(label1);
//                        block.add(loop1);
//                        breaks.put(commonDescendant, label1);
//                        inside = loop1.body;
//                    }
//
//                    Label label2 = new Label("loop" + loopCounter++);
//                    WhileLoop loop2 = new WhileLoop(new BooleanLiteral(true), new Block());
//                    inside.add(label2);
//                    inside.add(loop2);
//                    If ifExpr = new If(node.condition, new Block(), null);
//                    loop2.body.add(ifExpr);
//                    loop2.body.add(new Break(label2));
//                    breaks.put(node.next, label2);
//
//                    nodeStack.push(node.ifTrue);
//                    blockStack.push(ifExpr.ifBlock);
//
//                    nodeStack.push(node.next);
//                    blockStack.push(inside);
//
//                    if (commonDescendant != null) {
//                        nodeStack.push(commonDescendant);
//                        blockStack.push(block); // visit before visiting branches
//                    }
//                    continue;
//                }
//            }
//
//            if (nextBreak != null) {
//                block.statements.add(new Break(nextBreak));
//                continue;
//            }
//
//            if (nextContinue != null) {
//                block.statements.add(new Continue(nextContinue));
//                continue;
//            }
//
//            nodeStack.push(node.next);
//            blockStack.push(block);
//        }
//
//        return result;
//    }
//
//    @SuppressWarnings("TailRecursion")
//    private ControlFlowNode getCommonDescendant(ControlFlowNode a, ControlFlowNode b, Set<ControlFlowNode> visited) {
//        if (a == null) {
//            return b;
//        }
//
//        if (b == null) {
//            return a;
//        }
//
//        if (a == b) {
//            return a;
//        }
//
//        if (!visited.add(a) || !visited.add(b)) {
//            return null;
//        }
//
//        a = getCommonDescendant(a.next, a.ifTrue, visited);
//
//        if (a == b) {
//            return a;
//        }
//
//        b = getCommonDescendant(b.next, b.ifTrue, visited);
//
//        if (a == b) {
//            return a;
//        }
//
//        return getCommonDescendant(a, b, visited);
//    }
//}
