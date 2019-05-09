package uncompile.astbuilder;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uncompile.ast.*;
import uncompile.controlflow.ControlFlowGraph;
import uncompile.controlflow.ControlFlowNode;
import uncompile.controlflow.Jump;
import uncompile.controlflow.ControlFlowGenerator;
import uncompile.metadata.PrimitiveType;
import uncompile.util.FakeMap;

import java.util.*;

public class MethodBuilder extends MethodNode {
    private final Method method;
    private final String className;
    private final String superName;

    private final HashMap<Integer, VariableDeclaration> indexToParameter = new HashMap<>();
    private final Set<VariableDeclaration> locals = new HashSet<>();

    private Set<Label> usefulLabels = new HashSet<>();

    private int variableCounter = 0;
    private int labelCounter = 0;
    private DescriptionProvider descriptionProvider;
    private Map<Label, ControlFlowBlock> labelToBlock = new HashMap<>();

    public MethodBuilder(Method method, String className, String superName, int access, String name, String descriptor, String signature, String[] exceptions, DescriptionProvider descriptionProvider) {
        super(Opcodes.ASM7, access, name, descriptor, signature, exceptions);

        this.method = method;
        this.className = className;
        this.superName = superName;
        this.descriptionProvider = descriptionProvider;

        int index = method.isStatic ? 0 : 1;
        for (VariableDeclaration parameter : method.parameters) {
            indexToParameter.put(index, parameter);
            index += getVariableSize(parameter);
        }

        if (!method.isAbstract) {
            method.body = new Block();
        }
    }

    private int getVariableSize(VariableDeclaration parameter) {
        if (parameter.type instanceof PrimitiveTypeNode) {
            PrimitiveType primitiveType = ((PrimitiveTypeNode) parameter.type).primitiveType;
            return primitiveType == PrimitiveType.LONG || primitiveType == PrimitiveType.DOUBLE ? 2 : 1;
        }

        return 1;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        usefulLabels.add(((JumpInsnNode) instructions.getLast()).label.getLabel());
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        TableSwitchInsnNode insn = (TableSwitchInsnNode) instructions.getLast();
        for (LabelNode labelNode : insn.labels) {
            usefulLabels.add(labelNode.getLabel());
        }
        usefulLabels.add(insn.dflt.getLabel());
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        LookupSwitchInsnNode insn = (LookupSwitchInsnNode) instructions.getLast();
        for (LabelNode labelNode : insn.labels) {
            usefulLabels.add(labelNode.getLabel());
        }
        usefulLabels.add(insn.dflt.getLabel());
    }

    @Override
    public void visitEnd() {
        if (method.isAbstract) {
            return;
        }

        Map<Label, Deque<TryCatchBlockNode>> tryCatchStarts = new HashMap<>();
        Map<Label, Deque<TryCatchBlockNode>> tryCatchEnds = new HashMap<>();
        Map<Label, TryCatchBlockNode> tryCatchHandlers = new HashMap<>();
        // TODO: do try catch ordering: make sure that all trys starting at the same label are listed in tryCatchStarts in the
        //  reverse order they end in.
        for (TryCatchBlockNode tryCatchBlock : tryCatchBlocks) {
            tryCatchStarts.computeIfAbsent(tryCatchBlock.start.getLabel(), k -> new ArrayDeque<>()).addFirst(tryCatchBlock);
            tryCatchEnds.computeIfAbsent(tryCatchBlock.end.getLabel(), k -> new ArrayDeque<>()).addLast(tryCatchBlock);
            tryCatchHandlers.put(tryCatchBlock.handler.getLabel(), tryCatchBlock);
        }
        usefulLabels.addAll(tryCatchStarts.keySet());
        usefulLabels.addAll(tryCatchEnds.keySet());
        usefulLabels.addAll(tryCatchHandlers.keySet());

        ControlFlowBlock startBlock = new ControlFlowBlock();
        ControlFlowBlock currentBlock = startBlock;
        List<ControlFlowBlock> blocks = new ArrayList<>();
        blocks.add(currentBlock);

        for (AbstractInsnNode insn : instructions.toArray()) {
            int opcode = insn.getOpcode();

            if (insn instanceof LabelNode && usefulLabels.contains(((LabelNode) insn).getLabel())) {
                Label label = ((LabelNode) insn).getLabel();

                ControlFlowBlock newBlock = labelToBlock.computeIfAbsent(label, k -> new ControlFlowBlock());
                currentBlock.nextBlocks.add(newBlock);
                currentBlock.successor = newBlock;
                labelToBlock.put(label, newBlock);
                currentBlock = newBlock;
                blocks.add(newBlock);
            }

            currentBlock.instructions.add(insn.clone(new FakeMap<>(LabelNode.class, l -> l)));

            if (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN ||
                opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN) {
                currentBlock.skipSaveFrame = true;
                ControlFlowBlock newBlock = new ControlFlowBlock();
                currentBlock = newBlock;
                blocks.add(newBlock);
            }

            if (insn instanceof JumpInsnNode) {
                Label label = ((JumpInsnNode) insn).label.getLabel();
                currentBlock.nextBlocks.add(labelToBlock.computeIfAbsent(label, k -> new ControlFlowBlock()));

                ControlFlowBlock newBlock = new ControlFlowBlock();
                if (opcode != Opcodes.GOTO) {
                    currentBlock.successor = newBlock;
                    currentBlock.nextBlocks.add(newBlock);
                }
                currentBlock = newBlock;
                blocks.add(newBlock);
            }

            if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
                List<LabelNode> labels = insn instanceof TableSwitchInsnNode ?
                        ((TableSwitchInsnNode) insn).labels :
                        ((LookupSwitchInsnNode) insn).labels;
                labels.add(insn instanceof TableSwitchInsnNode ?
                        ((TableSwitchInsnNode) insn).dflt :
                        ((LookupSwitchInsnNode) insn).dflt);

                for (LabelNode labelNode : labels) {
                    Label label = labelNode.getLabel();
                    currentBlock.nextBlocks.add(labelToBlock.computeIfAbsent(label, k -> new ControlFlowBlock()));
                }

                ControlFlowBlock newBlock = new ControlFlowBlock();
                currentBlock = newBlock;
                blocks.add(newBlock);
            }
        }

        // Assign start and end frames to blocks such that the start frame of
        // any block is equal with the end frames of all incoming blocks
        for (ControlFlowBlock block : blocks) {
            if (block.startFrame == null) {
                setBlockStartFrame(blocks, block, new Frame());
            }

            if (block.endFrame == null) {
                setBlockEndFrame(blocks, block, new Frame());
            }
        }

        // Determine blocks that must be visited before a certain block such that
        // it is known which local variables in the start frame have been declared.
        Deque<ControlFlowBlock> stack = new ArrayDeque<>();
        stack.push(startBlock);
        Set<ControlFlowBlock> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            ControlFlowBlock block = stack.pop();
            if (!visited.add(block)) {
                continue;
            }
            for (ControlFlowBlock nextBlock : block.nextBlocks) {
                nextBlock.unknownIncomingFrames.add(block);
            }
        }

        // Create a control flow node for each block
        ControlFlowGraph graph = new ControlFlowGraph();
        for (ControlFlowBlock block : blocks) {
            block.node = graph.createNode();
        }

        for (ControlFlowBlock block : blocks) {
            block.successorNode = block.successor == null ? null : block.successor.node;
        }

        // Build
        buildBlockAst(startBlock, new BlockBuilder(method, className, superName, indexToParameter, () -> variableCounter++, locals, this::getLabelTarget, descriptionProvider, maxLocals, startBlock.successorNode));

        // Copy the AST for each block into the node
        for (ControlFlowBlock block : blocks) {
            block.node.block = block.block == null ? new Block() : block.block;
            if (block.jump == null) {
                block.jump = block.successorNode == null ? new Jump.None() : new Jump.Unconditional(block.successorNode);
            }
            block.node.setJump(block.jump);
        }

        List<Statement> statements = new ArrayList<>();
//        Deque<List<Statement>> tryCatchStack = new ArrayDeque<>();
//        Deque<TryCatchBlockNode> openTryCatches = new ArrayDeque<>();
//        for (ControlFlowBlock block : blocks) {
//            // Create try-catch block if necessary
//            if (block.instructions.size() != 0 && block.instructions.getFirst() instanceof LabelNode) {
//                Label label = ((LabelNode) block.instructions.getFirst()).getLabel();
//                for (TryCatchBlockNode tryCatchBlock : tryCatchStarts.getOrDefault(label, EMPTY_DEQUE)) {
//                    openTryCatches.push(tryCatchBlock);
//                    Block tryBlock = new Block();
//                    TryCatch astTryCatchBlock = new TryCatch(tryBlock);
//                    statements.add(astTryCatchBlock);
//                    tryCatchStack.push(statements);
//                    statements = tryBlock.statements;
//                    if (tryCatchBlock.type != null) {
//                        TryCatch.Catch catchBlock = new TryCatch.Catch(new VariableDeclaration( // TODO
//                                new ClassReference(new ClassType(tryCatchBlock.type.replace('/', '.'))),
//                                "e" + variableCounter++,
//                                false,
//                                false,
//                                false
//                        ), new Block());
//                        astTryCatchBlock.catchBlocks.add(catchBlock);
//                        catchBlock.exceptionTypes.add(new ClassReference(new ClassType(tryCatchBlock.type.replace('/', '.'))));
//                        catchBlock.block.statements.add(new Goto(getLabelTarget(tryCatchBlock.handler.getLabel()), null));
//                    } else { // finally
//                        astTryCatchBlock.finallyBlock.statements.add(new Goto(getLabelTarget(tryCatchBlock.handler.getLabel()), null));
//                    }
//                }
//
//                for (TryCatchBlockNode tryCatchBlock : tryCatchEnds.getOrDefault(label, EMPTY_DEQUE)) {
//                    if (openTryCatches.isEmpty()) {
//                        throw new DecompilationNotPossibleException("try block ended before it started");
//                    }
//
//                    TryCatchBlockNode lastTryCatch = openTryCatches.pop();
//                    if (lastTryCatch != tryCatchBlock) {
//                        throw new DecompilationNotPossibleException("try block ended is not last one started, this is valid bytecode, but no " +
//                                                                    "corresponding Java code exists");
//                    }
//
//                    statements = tryCatchStack.pop();
//                }
//            }
//
//            // Add expressions in that block
//            if (block.block != null) {
//                statements.add(block.block);
//            } else if (!DecompilationSettings.IGNORE_UNREACHABLE_CODE) {
//                for (AbstractInsnNode insn : block.instructions.toArray()) {
//                    if (!(insn instanceof LabelNode)) {
//                        throw new DecompilationNotPossibleException("unreachable code (or decompiler bug)");
//                    }
//                }
//            }
//        }
//
//        if (!openTryCatches.isEmpty()) {
//            throw new DecompilationNotPossibleException("try block not ended");
//        }

        graph.entryPoint = startBlock.node;
        method.body = new ControlFlowGenerator(graph).createCode();
        method.body.addExpressions(locals);
        method.body.addStatements(statements);
    }

    private void setBlockStartFrame(List<ControlFlowBlock> blocks, ControlFlowBlock block, Frame frame) {
        if (block.startFrame == null) {
            block.startFrame = frame;
            for (ControlFlowBlock otherBlock : blocks) {
                if (otherBlock.nextBlocks.contains(block)) {
                    setBlockEndFrame(blocks, otherBlock, frame);
                }
            }
        }
    }

    private void setBlockEndFrame(List<ControlFlowBlock> blocks, ControlFlowBlock block, Frame frame) {
        if (block.endFrame == null) {
            block.endFrame = frame;
            for (ControlFlowBlock otherBlock : block.nextBlocks) {
                setBlockStartFrame(blocks, otherBlock, frame);
            }

            for (ControlFlowBlock otherBlock : block.exceptionHandlers) {
                setBlockEndFrame(blocks, otherBlock, frame);
            }
        }
    }

    private ControlFlowNode getLabelTarget(Label label) {
        return labelToBlock.get(label).node;
    }

    private void buildBlockAst(ControlFlowBlock node, BlockBuilder blockBuilder) {
        blockBuilder.loadFrame(node.startFrame, node.uninitializedLocals);
        node.instructions.accept(blockBuilder);

        if (!node.skipSaveFrame) {
            blockBuilder.saveFrame(node.endFrame);
        }

        node.block = blockBuilder.getBlock();
        node.jump = blockBuilder.getJump();

        for (ControlFlowBlock nextNode : node.nextBlocks) {
            if (nextNode.block == null) {
                nextNode.unknownIncomingFrames.remove(node);
                for (int i = 0; i < maxLocals; i++) {
                    if (node.endFrame.locals[i] == null) {
                        nextNode.uninitializedLocals.add(i);
                    }
                }

                if (nextNode.unknownIncomingFrames.isEmpty()) {
                    buildBlockAst(nextNode, blockBuilder.createNewBuilder(nextNode.successorNode));
                }
            }
        }
    }

    private static class ControlFlowBlock {
        public InsnList instructions = new InsnList();
        public Frame startFrame = null;
        public Frame endFrame = null;
        public Set<ControlFlowBlock> unknownIncomingFrames = new HashSet<>();
        public Set<Integer> uninitializedLocals = new HashSet<>();
        public Set<ControlFlowBlock> nextBlocks = new LinkedHashSet<>();
        public Set<ControlFlowBlock> exceptionHandlers = new LinkedHashSet<>();
        public Block block = null;
        public boolean skipSaveFrame = false;
        public ControlFlowNode node = null;
        public ControlFlowBlock successor = null;
        public ControlFlowNode successorNode = null;
        public Jump jump = null;
    }

    private static int debugIdCounter = 0;

    public static class Frame {
        public int debugId = debugIdCounter++;
        public VariableDeclaration[] stack = null;
        public VariableDeclaration[] locals = null;
    }
}
