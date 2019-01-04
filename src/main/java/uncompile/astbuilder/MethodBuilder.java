package uncompile.astbuilder;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import uncompile.DecompilationNotPossibleException;
import uncompile.DecompilationSettings;
import uncompile.ast.*;
import uncompile.metadata.ClassType;
import uncompile.metadata.PrimitiveType;
import uncompile.util.FakeMap;

import java.util.*;

public class MethodBuilder extends MethodNode {
    private static final Deque<TryCatchBlockNode> EMPTY_DEQUE = new ArrayDeque<>();

    private final Method method;
    private final String className;
    private final String superName;

    private final HashMap<Integer, VariableDeclaration> indexToParameter = new HashMap<>();
    private final Set<VariableDeclaration> locals = new HashSet<>();

    private Set<Label> usefulLabels = new HashSet<>();

    private int variableCounter = 0;
    private int labelCounter = 0;
    private Map<Label, uncompile.ast.Label> labelMap = new HashMap<>();
    private DescriptionProvider descriptionProvider;

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

        Map<Label, ControlFlowBlock> labelToBlock = new HashMap<>();
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
                labelToBlock.put(label, newBlock);
                currentBlock = newBlock;
                blocks.add(newBlock);
            }

            currentBlock.instructions.add(insn.clone(new FakeMap<>(LabelNode.class, l -> l)));

            if (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN ||
                opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN) {
                ControlFlowBlock newBlock = new ControlFlowBlock();
                currentBlock.nextBlocks.add(newBlock);
                currentBlock = newBlock;
                blocks.add(newBlock);
            }

            if (insn instanceof JumpInsnNode) {
                currentBlock.saveFrameBeforeLast = true;
                Label label = ((JumpInsnNode) insn).label.getLabel();
                currentBlock.nextBlocks.add(labelToBlock.computeIfAbsent(label, k -> new ControlFlowBlock()));

                ControlFlowBlock newBlock = new ControlFlowBlock();
                if (opcode != Opcodes.GOTO) {
                    currentBlock.nextBlocks.add(newBlock);
                }
                currentBlock = newBlock;
                blocks.add(newBlock);
            }

            if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
                currentBlock.saveFrameBeforeLast = true;
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

        for (ControlFlowBlock block : blocks) {
            if (block.startFrame == null) {
                setBlockStartFrame(blocks, block, new Frame());
            }

            if (block.endFrame == null) {
                setBlockEndFrame(blocks, block, new Frame());
            }
        }

        buildBlockAST(startBlock, new BlockBuilder(method, className, superName, indexToParameter, () -> variableCounter++, locals, this::getAstLabel, descriptionProvider));

        List<Expression> expressions = new ArrayList<>();
        Deque<List<Expression>> tryCatchStack = new ArrayDeque<>();
        Deque<TryCatchBlockNode> openTryCatches = new ArrayDeque<>();
        for (ControlFlowBlock block : blocks) {
            // Create try-catch block if necessary
            if (block.instructions.size() != 0 && block.instructions.getFirst() instanceof LabelNode) {
                Label label = ((LabelNode) block.instructions.getFirst()).getLabel();
                for (TryCatchBlockNode tryCatchBlock : tryCatchStarts.getOrDefault(label, EMPTY_DEQUE)) {
                    openTryCatches.push(tryCatchBlock);
                    Block tryBlock = new Block();
                    TryCatch astTryCatchBlock = new TryCatch(tryBlock);
                    expressions.add(astTryCatchBlock);
                    tryCatchStack.push(expressions);
                    expressions = tryBlock.expressions;
                    if (tryCatchBlock.type != null) {
                        TryCatch.Catch catchBlock = new TryCatch.Catch(new VariableDeclaration( // TODO
                                new ClassReference(new ClassType(tryCatchBlock.type.replace('/', '.'))),
                                "e" + variableCounter++,
                                false,
                                false,
                                false
                        ), new Block());
                        astTryCatchBlock.catchBlocks.add(catchBlock);
                        catchBlock.exceptionTypes.add(new ClassReference(new ClassType(tryCatchBlock.type.replace('/', '.'))));
                        catchBlock.block.expressions.add(new Goto(getAstLabel(tryCatchBlock.handler.getLabel()), null));
                    } else { // finally
                        astTryCatchBlock.finallyBlock.expressions.add(new Goto(getAstLabel(tryCatchBlock.handler.getLabel()), null));
                    }
                }

                for (TryCatchBlockNode tryCatchBlock : tryCatchEnds.getOrDefault(label, EMPTY_DEQUE)) {
                    if (openTryCatches.isEmpty()) {
                        throw new DecompilationNotPossibleException("try block ended before it started");
                    }

                    TryCatchBlockNode lastTryCatch = openTryCatches.pop();
                    if (lastTryCatch != tryCatchBlock) {
                        throw new DecompilationNotPossibleException("try block ended is not last one started, this is valid bytecode, but no " +
                                                                    "corresponding Java code exists");
                    }

                    expressions = tryCatchStack.pop();
                }
            }

            // Add expressions in that block
            if (block.expressions != null) {
                expressions.addAll(block.expressions);
            } else if (block.instructions.size() != 0) {
                if (!DecompilationSettings.IGNORE_UNREACHABLE_CODE) {
                    throw new DecompilationNotPossibleException("unreachable code");
                }
            }
        }

        if (!openTryCatches.isEmpty()) {
            throw new DecompilationNotPossibleException("try block not ended");
        }

        method.body = new Block();
        method.body.expressions.addAll(locals);
        method.body.expressions.addAll(expressions);
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
        }
    }

    private uncompile.ast.Label getAstLabel(Label label) {
        return labelMap.computeIfAbsent(label, k -> new uncompile.ast.Label("label" + labelCounter++));
    }

    private void buildBlockAST(ControlFlowBlock block, BlockBuilder blockBuilder) {
        blockBuilder.loadFrame(block.startFrame);
        block.instructions.accept(blockBuilder);

        // Remove last expression, save frame before it, and add it back
        if (block.saveFrameBeforeLast) {
            Expression last = blockBuilder.getExpressions().remove(blockBuilder.getExpressions().size() - 1);
            blockBuilder.saveFrame(block.endFrame);
            blockBuilder.getExpressions().add(last);
        } else {
            blockBuilder.saveFrame(block.endFrame);
        }

        block.expressions = blockBuilder.getExpressions();

        for (ControlFlowBlock nextBlock : block.nextBlocks) {
            if (nextBlock.expressions == null) {
                buildBlockAST(nextBlock, blockBuilder.createNewBuilder());
            }
        }
    }

    private static class ControlFlowBlock {
        public InsnList instructions = new InsnList();
        public Frame startFrame = null;
        public Frame endFrame = null;
        public List<ControlFlowBlock> nextBlocks = new ArrayList<>();
        public List<Expression> expressions = null;
        public boolean saveFrameBeforeLast = false;
    }

    public static class Frame {
        public List<VariableDeclaration> stackVariables = null;
        // TODO: locals
    }
}
