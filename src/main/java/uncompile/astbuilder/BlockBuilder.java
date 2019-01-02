package uncompile.astbuilder;

import org.objectweb.asm.Label;
import org.objectweb.asm.*;
import uncompile.DecompilationNotPossibleException;
import uncompile.ast.*;
import uncompile.metadata.Type;
import uncompile.metadata.*;
import uncompile.util.DescriptorReader;

import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlockBuilder extends MethodVisitor {
    private final Method method;
    private final String className;
    private final String superName;
    private final Supplier<Integer> variableCounter;
    private final Set<VariableDeclaration> locals;
    private final Map<Integer, VariableDeclaration> indexToParameter;
    private final DescriptionProvider descriptionProvider;

    // State
    private Map<Integer, VariableDeclaration> currentLocals = new HashMap<>();
    private Deque<Expression> stack = new ArrayDeque<>();

    // Result
    private List<Expression> expressions = new ArrayList<>();
    private Function<Label, uncompile.ast.Label> astLabelProvider;

    public BlockBuilder(
            Method method,
            String className,
            String superName,
            Map<Integer, VariableDeclaration> indexToParameter,
            Supplier<Integer> variableCounter,
            Set<VariableDeclaration> locals,
            Function<Label, uncompile.ast.Label> astLabelProvider,
            DescriptionProvider descriptionProvider) {
        super(Opcodes.ASM7);
        this.method = method;
        this.className = className;
        this.superName = superName;
        this.variableCounter = variableCounter;
        this.locals = locals;
        this.indexToParameter = indexToParameter;
        this.astLabelProvider = astLabelProvider;
        this.descriptionProvider = descriptionProvider;
    }

    public void loadFrame(MethodBuilder.Frame frame) {
        if (!stack.isEmpty()) {
            throw new IllegalStateException("stack not empty");
        }

        if (frame.stackVariables == null) {
            return; // nothing to load
        }

        for (VariableDeclaration stackVariable : frame.stackVariables) {
            stack.addLast(new VariableReference(stackVariable));
        }
    }

    public void saveFrame(MethodBuilder.Frame frame) {
        if (frame.stackVariables == null) {
            frame.stackVariables = new ArrayList<>();
            for (Expression expression : stack) {
                frame.stackVariables.add(createTemporaryVariable(expression));
            }
        } else {
            if (frame.stackVariables.size() != stack.size()) {
                throw new DecompilationNotPossibleException("frame size doesn't match");
            }

            int i = 0;
            for (Expression expression : stack) {
                expressions.add(new Assignment(new VariableReference(frame.stackVariables.get(i++)), expression));
            }
        }

        stack.clear();
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public BlockBuilder createNewBuilder() {
        BlockBuilder newBuilder = new BlockBuilder(method, className, superName, indexToParameter, variableCounter, locals, astLabelProvider, descriptionProvider);
        newBuilder.currentLocals = new HashMap<>(currentLocals);
        newBuilder.stack = new ArrayDeque<>(stack);
        return newBuilder;
    }

    private Expression getVariableReference(int var) {
        if (!method.isStatic && var == 0) {
            return new ThisReference(new ClassReference(method.owner.getClassType()), false);
        }

        VariableDeclaration parameter = indexToParameter.get(var);
        if (parameter != null) {
            return new VariableReference(parameter);
        }

        // TODO: find in LVT

        return new VariableReference(currentLocals.get(var));
    }

    private VariableDeclaration createTemporaryVariable(Expression store, Type type) {
        VariableDeclaration variable = new VariableDeclaration(
                TypeNode.fromType(type),
                "tmp" + variableCounter.get(),
                false,
                true,
                false
        );

        locals.add(variable);
        expressions.add(new Assignment(new VariableReference(variable), store));
        return variable;
    }

    private VariableDeclaration createTemporaryVariable(Expression store) {
        return createTemporaryVariable(store, store.getType());
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case Opcodes.NOP: {
                break;
            }

            case Opcodes.ACONST_NULL: {
                stack.push(new NullLiteral());
                break;
            }

            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5: {
                stack.push(new IntLiteral(opcode - Opcodes.ICONST_0));
                break;
            }

            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1: {
                stack.push(new LongLiteral(opcode - Opcodes.LCONST_0));
                break;
            }

            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2: {
                stack.push(new FloatLiteral(opcode - Opcodes.FCONST_0));
                break;
            }

            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1: {
                stack.push(new FloatLiteral(opcode - Opcodes.DCONST_0));
                break;
            }

            case Opcodes.RETURN: {
                expressions.add(new Return(null));
                break;
            }

            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN: {
                expressions.add(new Return(stack.pop()));
                break;
            }

            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD: {
                Expression index = stack.pop();
                Expression array = stack.pop();
                ArrayElement expression = new ArrayElement(array, index);

                Type type = null;
                if (opcode == Opcodes.IALOAD) type = PrimitiveType.INT;
                if (opcode == Opcodes.LALOAD) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FALOAD) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DALOAD) type = PrimitiveType.DOUBLE;
                if (opcode == Opcodes.BALOAD) type = PrimitiveType.BYTE;
                if (opcode == Opcodes.CALOAD) type = PrimitiveType.CHAR;
                if (opcode == Opcodes.SALOAD) type = PrimitiveType.SHORT;
                if (opcode == Opcodes.AALOAD) type = expression.getType();

                stack.push(new VariableReference(createTemporaryVariable(expression, type)));
                break;
            }

            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE: {
                Expression value = stack.pop();
                Expression index = stack.pop();
                Expression array = stack.pop();
                ArrayElement expression = new ArrayElement(array, index);

                expressions.add(new Assignment(expression, value));
                break;
            }

            case Opcodes.POP: {
                stack.pop();
                break;
            }

            case Opcodes.POP2: {
                stack.pop();
                stack.pop();
                break;
            }

            case Opcodes.DUP: {
                Expression toDuplicate = stack.pop();
                VariableDeclaration result = createTemporaryVariable(toDuplicate);

                stack.push(new VariableReference(result));
                stack.push(new VariableReference(result));
                break;
            }

            case Opcodes.DUP_X1: {
                Expression x = stack.pop();
                Expression dup = stack.pop();
                VariableDeclaration dupVar = createTemporaryVariable(dup);

                stack.push(new VariableReference(dupVar));
                stack.push(x);
                stack.push(new VariableReference(dupVar));
                break;
            }

            case Opcodes.DUP_X2: {
                Expression x2 = stack.pop();
                Expression x1 = stack.pop();
                Expression toDuplicate = stack.pop();
                VariableDeclaration result = createTemporaryVariable(toDuplicate);

                stack.push(new VariableReference(result));
                stack.push(x1);
                stack.push(x2);
                stack.push(new VariableReference(result));
                break;
            }

            case Opcodes.DUP2: {
                Expression dup2 = stack.pop();
                Expression dup1 = stack.pop();
                VariableDeclaration dupVar2 = createTemporaryVariable(dup2);
                VariableDeclaration dupVar1 = createTemporaryVariable(dup1);

                stack.push(new VariableReference(dupVar1));
                stack.push(new VariableReference(dupVar2));
                stack.push(new VariableReference(dupVar1));
                stack.push(new VariableReference(dupVar2));
                break;
            }

            case Opcodes.DUP2_X1: {
                Expression dup2 = stack.pop();
                Expression dup1 = stack.pop();
                Expression x = stack.pop();
                VariableDeclaration dupVar2 = createTemporaryVariable(dup2);
                VariableDeclaration dupVar1 = createTemporaryVariable(dup1);

                stack.push(new VariableReference(dupVar1));
                stack.push(new VariableReference(dupVar2));
                stack.push(x);
                stack.push(new VariableReference(dupVar1));
                stack.push(new VariableReference(dupVar2));
                break;
            }

            case Opcodes.DUP2_X2: {
                Expression dup2 = stack.pop();
                Expression dup1 = stack.pop();
                Expression x2 = stack.pop();
                Expression x1 = stack.pop();
                VariableDeclaration dupVar2 = createTemporaryVariable(dup2);
                VariableDeclaration dupVar1 = createTemporaryVariable(dup1);

                stack.push(new VariableReference(dupVar1));
                stack.push(new VariableReference(dupVar2));
                stack.push(x1);
                stack.push(x2);
                stack.push(new VariableReference(dupVar1));
                stack.push(new VariableReference(dupVar2));
                break;
            }

            case Opcodes.SWAP: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(value2);
                stack.push(value1);
                break;
            }

            case Opcodes.IADD:
            case Opcodes.LADD:
            case Opcodes.FADD:
            case Opcodes.DADD: {
                Type type = null;
                if (opcode == Opcodes.IADD) type = PrimitiveType.INT;
                if (opcode == Opcodes.LADD) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FADD) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DADD) type = PrimitiveType.DOUBLE;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.ADD, value1, value2), type)));
                break;
            }

            case Opcodes.ISUB:
            case Opcodes.LSUB:
            case Opcodes.FSUB:
            case Opcodes.DSUB: {
                Type type = null;
                if (opcode == Opcodes.ISUB) type = PrimitiveType.INT;
                if (opcode == Opcodes.LSUB) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FSUB) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DSUB) type = PrimitiveType.DOUBLE;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.SUBTRACT, value1, value2), type)));
                break;
            }

            case Opcodes.IMUL:
            case Opcodes.LMUL:
            case Opcodes.FMUL:
            case Opcodes.DMUL: {
                Type type = null;
                if (opcode == Opcodes.IMUL) type = PrimitiveType.INT;
                if (opcode == Opcodes.LMUL) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FMUL) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DMUL) type = PrimitiveType.DOUBLE;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.MULTIPLY, value1, value2), type)));
                break;
            }

            case Opcodes.IDIV:
            case Opcodes.LDIV:
            case Opcodes.FDIV:
            case Opcodes.DDIV: {
                Type type = null;
                if (opcode == Opcodes.IDIV) type = PrimitiveType.INT;
                if (opcode == Opcodes.LDIV) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FDIV) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DDIV) type = PrimitiveType.DOUBLE;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.DIVIDE, value1, value2), type)));
                break;
            }

            case Opcodes.IREM:
            case Opcodes.LREM:
            case Opcodes.FREM:
            case Opcodes.DREM: {
                Type type = null;
                if (opcode == Opcodes.IREM) type = PrimitiveType.INT;
                if (opcode == Opcodes.LREM) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FREM) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DREM) type = PrimitiveType.DOUBLE;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.REMAINDER, value1, value2), type)));
                break;
            }

            case Opcodes.INEG:
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG: {
                Type type = null;
                if (opcode == Opcodes.INEG) type = PrimitiveType.INT;
                if (opcode == Opcodes.LNEG) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FNEG) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DNEG) type = PrimitiveType.DOUBLE;

                stack.push(new VariableReference(createTemporaryVariable(new UnaryOperation(UnaryOperator.MINUS, stack.pop()), type)));
                break;
            }

            case Opcodes.ISHL:
            case Opcodes.LSHL: {
                Type type = null;
                if (opcode == Opcodes.ISHL) type = PrimitiveType.INT;
                if (opcode == Opcodes.LSHL) type = PrimitiveType.LONG;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.LEFT_SHIFT, value1, value2), type)));
                break;
            }

            case Opcodes.ISHR:
            case Opcodes.LSHR: {
                Type type = null;
                if (opcode == Opcodes.ISHR) type = PrimitiveType.INT;
                if (opcode == Opcodes.LSHR) type = PrimitiveType.LONG;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.RIGHT_SHIFT, value1, value2), type)));
                break;
            }

            case Opcodes.IUSHR:
            case Opcodes.LUSHR: {
                Type type = null;
                if (opcode == Opcodes.IUSHR) type = PrimitiveType.INT;
                if (opcode == Opcodes.LUSHR) type = PrimitiveType.LONG;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.UNSIGNED_RIGHT_SHIFT, value1, value2), type)));
                break;
            }

            case Opcodes.IAND:
            case Opcodes.LAND: {
                Type type = null;
                if (opcode == Opcodes.IAND) type = PrimitiveType.INT;
                if (opcode == Opcodes.LAND) type = PrimitiveType.LONG;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.BITWISE_AND, value1, value2), type)));
                break;
            }

            case Opcodes.IOR:
            case Opcodes.LOR: {
                Type type = null;
                if (opcode == Opcodes.IOR) type = PrimitiveType.INT;
                if (opcode == Opcodes.LOR) type = PrimitiveType.LONG;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.BITWISE_OR, value1, value2), type)));
                break;
            }

            case Opcodes.IXOR:
            case Opcodes.LXOR: {
                Type type = null;
                if (opcode == Opcodes.IXOR) type = PrimitiveType.INT;
                if (opcode == Opcodes.LXOR) type = PrimitiveType.LONG;

                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                stack.push(new VariableReference(createTemporaryVariable(new BinaryOperation(BinaryOperator.BITWISE_XOR, value1, value2), type)));
                break;
            }

            case Opcodes.ARRAYLENGTH: {
                stack.push(new VariableReference(createTemporaryVariable(new ArrayLength(stack.pop()))));
                break;
            }

            case Opcodes.ATHROW: {
                expressions.add(new Throw(stack.pop()));
                break;
            }

            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.LONG)));
                break;
            }

            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.FLOAT)));
                break;
            }

            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.FLOAT)));
                break;
            }

            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.INT)));
                break;
            }

            case Opcodes.I2B: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.BYTE)));
                break;
            }

            case Opcodes.I2C: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.CHAR)));
                break;
            }

            case Opcodes.I2S: {
                stack.push(new Cast(stack.pop(), TypeNode.fromType(PrimitiveType.SHORT)));
                break;
            }

            case Opcodes.LCMP:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG: {
                throw new UnsupportedOperationException("not yet implemented"); // TODO
            }

            case Opcodes.MONITORENTER: {
                throw new UnsupportedOperationException("not yet implemented"); // TODO
            }

            case Opcodes.MONITOREXIT: {
                throw new UnsupportedOperationException("not yet implemented"); // TODO
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        switch (opcode) {
            case Opcodes.BIPUSH: {
                stack.push(new IntLiteral(operand));
                break;
            }

            case Opcodes.SIPUSH: {
                stack.push(new Cast(new IntLiteral(operand), TypeNode.fromType(PrimitiveType.SHORT)));
                break;
            }

            case Opcodes.NEWARRAY: {
                stack.push(new VariableReference(createTemporaryVariable(new ArrayConstructor(TypeNode.fromType(getNewArrayType(operand)), new Expression[]{stack.pop()}))));
                break;
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        switch (opcode) {
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD: {
                stack.push(getVariableReference(var));
                break;
            }

            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE: {
                Expression value = stack.pop();

                Type type = null;
                if (opcode == Opcodes.ISTORE) type = PrimitiveType.INT;
                if (opcode == Opcodes.LSTORE) type = PrimitiveType.LONG;
                if (opcode == Opcodes.FSTORE) type = PrimitiveType.FLOAT;
                if (opcode == Opcodes.DSTORE) type = PrimitiveType.DOUBLE;
                if (opcode == Opcodes.ASTORE) type = value.getType();

                VariableDeclaration newVariable = new VariableDeclaration(
                        TypeNode.fromType(type),
                        "var" + variableCounter.get(),
                        false,
                        false,
                        false
                );
                locals.add(newVariable);
                currentLocals.put(var, newVariable);

                VariableReference reference = new VariableReference(newVariable);

                expressions.add(new Assignment(reference, value));
                break;
            }

            case Opcodes.RET: {
                throw new UnsupportedOperationException("not yet implemented");
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        ClassType classType = new ClassType(type.replace('/', '.'));

        switch (opcode) {
            case Opcodes.NEW: {
                stack.push(new VariableReference(createTemporaryVariable(new NewInstance(new ClassReference(classType)), classType)));
                break;
            }

            case Opcodes.ANEWARRAY: {
                stack.push(new VariableReference(createTemporaryVariable(new ArrayConstructor(new ClassReference(classType), new Expression[]{stack.pop()}))));
                break;
            }

            case Opcodes.CHECKCAST: {
                Cast asObject = new Cast(stack.pop(), new ClassReference(ClassType.OBJECT));
                Cast asTargetClass = new Cast(asObject, new ClassReference(classType));
                stack.push(new VariableReference(createTemporaryVariable(asTargetClass, classType)));
                break;
            }

            case Opcodes.INSTANCEOF: {
                Cast asObject = new Cast(stack.pop(), new ClassReference(ClassType.OBJECT));
                BinaryOperation op = new BinaryOperation(BinaryOperator.INSTANCEOF, asObject, new ClassReference(classType));
                stack.push(new VariableReference(createTemporaryVariable(op, PrimitiveType.BOOLEAN)));
                break;
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        ClassType ownerType = new ClassType(owner.replace('/', '.'));
        Type fieldType = new DescriptorReader(descriptor, 0).read();
        boolean isStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
        FieldDescription field = descriptionProvider.getFieldDescription(owner, name, descriptor, isStatic);

        switch (opcode) {
            case Opcodes.GETSTATIC: {
                stack.push(new VariableReference(createTemporaryVariable(new StaticFieldReference(new ClassReference(ownerType), field), fieldType)));
                break;
            }
            case Opcodes.GETFIELD: {
                Expression ownerExpr = stack.pop();
                if (ownerExpr instanceof ThisReference && !owner.equals(className)) {
                    ownerExpr = new SuperReference(new ClassReference(ownerType), false);
                }

                stack.push(new VariableReference(createTemporaryVariable(new InstanceFieldReference(ownerExpr, field), fieldType)));
                break;
            }

            case Opcodes.PUTSTATIC: {
                expressions.add(new Assignment(new StaticFieldReference(new ClassReference(ownerType), field), stack.pop()));
                break;
            }

            case Opcodes.PUTFIELD: {
                Expression value = stack.pop();

                Expression ownerExpr = stack.pop();
                if (ownerExpr instanceof ThisReference && !owner.equals(className)) {
                    ownerExpr = new SuperReference(new ClassReference(ownerType), false);
                }

                expressions.add(new Assignment(new InstanceFieldReference(ownerExpr, field), value));
                break;
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        ClassType currentClassType = new ClassType(className.replace('/', '.'));
        ClassType ownerType = new ClassType(owner.replace('/', '.'));

        DescriptorReader r = new DescriptorReader(descriptor, 1);
        List<Type> parameterTypes = new ArrayList<>();
        while (descriptor.charAt(r.pos) != ')') {
            parameterTypes.add(r.read());
        }
        r.pos++;
        Type returnType = r.read();

        boolean isStatic = opcode == Opcodes.INVOKESTATIC;
        MethodDescription method = descriptionProvider.getMethodDescription(owner, name, descriptor, isStatic);

        // Get arguments
        List<Expression> arguments = new ArrayList<>();
        for (Type type : parameterTypes) {
            arguments.add(new Cast(stack.pop(), TypeNode.fromType(type)));
        }

        Expression thisArgument = !isStatic ? stack.pop() : null;

        switch (opcode) {
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE: {
                InstanceMethodCall call = new InstanceMethodCall(new Par(new Cast(thisArgument, TypeNode.fromType(ownerType))), method);
                call.arguments = arguments;

                if (returnType.equals(PrimitiveType.VOID)) {
                    expressions.add(call);
                } else {
                    stack.push(new VariableReference(createTemporaryVariable(call, returnType)));
                }

                break;
            }

            case Opcodes.INVOKESPECIAL: {
                if (owner.equals(className)) {
                    // do nothing
                } else if (owner.equals(superName) && thisArgument instanceof ThisReference) {
                    thisArgument = new SuperReference(new ClassReference(currentClassType), isInterface);
                } else if (!name.equals("<init>")) {
                    throw new DecompilationNotPossibleException("invokespecial called on non-constructor in non-this/super class");
                }

                // Constructor
                if (name.equals("<init>")) {
                    if (thisArgument instanceof ThisReference) {
                        ThisConstructorCall call = new ThisConstructorCall((ThisReference) thisArgument);
                        call.arguments = arguments;
                        expressions.add(call);
                    } else if (thisArgument instanceof SuperReference) {
                        SuperConstructorCall call = new SuperConstructorCall((SuperReference) thisArgument);
                        call.arguments = arguments;
                        expressions.add(call);
                    } else {
                        ConstructorCall call = new ConstructorCall(new ClassReference(ownerType), method);
                        call.arguments = arguments;
                        expressions.add(new Assignment(thisArgument, call));
                    }
                    break;
                }

                // Private method in this class, or method in super class
                // TODO: throw exception if in this class and not private to avoid producing incorrect code
                InstanceMethodCall call = new InstanceMethodCall(thisArgument, method);
                call.arguments = arguments;

                if (returnType.equals(PrimitiveType.VOID)) {
                    expressions.add(call);
                } else {
                    stack.push(new VariableReference(createTemporaryVariable(call, returnType)));
                }

                break;
            }

            case Opcodes.INVOKESTATIC: {
                StaticMethodCall call = new StaticMethodCall(new ClassReference(ownerType), method);
                call.arguments = arguments;

                if (returnType.equals(PrimitiveType.VOID)) {
                    expressions.add(call);
                } else {
                    stack.push(new VariableReference(createTemporaryVariable(call, returnType)));
                }

                break;
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        uncompile.ast.Label astLabel = astLabelProvider.apply(label);

        switch (opcode) {
            case Opcodes.IFEQ: {
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.EQ, stack.pop(), new IntLiteral(0))));
                break;
            }

            case Opcodes.IFNE: {
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.NE, stack.pop(), new IntLiteral(0))));
                break;
            }

            case Opcodes.IFLT: {
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.LT, stack.pop(), new IntLiteral(0))));
                break;
            }

            case Opcodes.IFGE: {
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.GE, stack.pop(), new IntLiteral(0))));
                break;
            }

            case Opcodes.IFGT: {
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.GT, stack.pop(), new IntLiteral(0))));
                break;
            }

            case Opcodes.IFLE: {
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.LE, stack.pop(), new IntLiteral(0))));
                break;
            }

            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ACMPEQ: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.EQ, value1, value2)));
                break;
            }

            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ACMPNE: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.NE, value1, value2)));
                break;
            }

            case Opcodes.IF_ICMPLT: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.LT, value1, value2)));
                break;
            }

            case Opcodes.IF_ICMPGE: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.GE, value1, value2)));
                break;
            }

            case Opcodes.IF_ICMPGT: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.GT, value1, value2)));
                break;
            }

            case Opcodes.IF_ICMPLE: {
                Expression value2 = stack.pop();
                Expression value1 = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.LE, value1, value2)));
                break;
            }

            case Opcodes.GOTO: {
                expressions.add(new Goto(astLabel, null));
                break;
            }

            case Opcodes.JSR: {
                throw new UnsupportedOperationException("not yet implemented");
            }

            case Opcodes.IFNULL: {
                Expression value = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.EQ, value, new NullLiteral())));
                break;
            }

            case Opcodes.IFNONNULL: {
                Expression value = stack.pop();
                expressions.add(new Goto(astLabel, new BinaryOperation(BinaryOperator.NE, value, new NullLiteral())));
                break;
            }

            default: {
                throw new AssertionError("opcode " + opcode);
            }
        }
    }

    @Override
    public void visitLabel(Label label) {
        expressions.add(astLabelProvider.apply(label));
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof String) {
            stack.push(new StringLiteral((String) value));
        } else if (value instanceof Integer) {
            stack.push(new IntLiteral((Integer) value));
        } else if (value instanceof Long) {
            stack.push(new LongLiteral((Long) value));
        } else if (value instanceof Float) {
            stack.push(new FloatLiteral((Float) value));
        } else if (value instanceof Double) {
            stack.push(new DoubleLiteral((Double) value));
        } else if (value instanceof org.objectweb.asm.Type) {
            org.objectweb.asm.Type type = (org.objectweb.asm.Type) value;
            if (type.getSort() == org.objectweb.asm.Type.OBJECT ||
                type.getSort() == org.objectweb.asm.Type.ARRAY) {
                stack.push(new ClassLiteral(TypeNode.fromType(new DescriptorReader(type.getDescriptor(), 0).read())));
            } else if (type.getSort() == org.objectweb.asm.Type.METHOD) {
                throw new UnsupportedOperationException("not yet implemented");
            } else {
                throw new AssertionError("sort " + type.getSort());
            }
        } else if (value instanceof MethodHandle) {
            throw new UnsupportedOperationException("not yet implemented");
        } else if (value instanceof ConstantDynamic) {
            throw new UnsupportedOperationException("not yet implemented");
        } else {
            throw new AssertionError("value " + value);
        }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        expressions.add(new Assignment(getVariableReference(var), new BinaryOperation(BinaryOperator.ADD, getVariableReference(var), new IntLiteral(increment))));
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        int[] keys = new int[max - min + 1];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = min + i;
        }

        visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        Switch switchExpr = new Switch(
                stack.pop(),
                new Expression[labels.length + 1],
                new Block[labels.length + 1]
        );

        for (int i = 0; i < labels.length; i++) {
            switchExpr.cases[i] = new IntLiteral(keys[i]);
            Block branch = new Block();
            branch.add(new Goto(astLabelProvider.apply(labels[i]), null));
            switchExpr.branches[i] = branch;
        }

        Block defaultBranch = new Block();
        defaultBranch.add(new Goto(astLabelProvider.apply(dflt), null));
        switchExpr.branches[switchExpr.branches.length - 1] = defaultBranch;

        expressions.add(switchExpr);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        Type componentType = new DescriptorReader(descriptor, 0).read();
        for (int i = 0; i < numDimensions; i++) {
            componentType = ((ArrayType) componentType).getComponentType();
        }

        Expression[] dimensions = new Expression[numDimensions];
        for (int i = numDimensions - 1; i >= 0; i--) {
            dimensions[i] = stack.pop();
        }

        stack.push(new VariableReference(createTemporaryVariable(new ArrayConstructor(TypeNode.fromType(componentType), dimensions))));
    }

    private PrimitiveType getNewArrayType(int operand) {
        PrimitiveType type;
        switch (operand) {
            case Opcodes.T_BOOLEAN: {
                type = PrimitiveType.BOOLEAN;
                break;
            }

            case Opcodes.T_CHAR: {
                type = PrimitiveType.CHAR;
                break;
            }

            case Opcodes.T_FLOAT: {
                type = PrimitiveType.FLOAT;
                break;
            }

            case Opcodes.T_DOUBLE: {
                type = PrimitiveType.DOUBLE;
                break;
            }

            case Opcodes.T_BYTE: {
                type = PrimitiveType.BYTE;
                break;
            }

            case Opcodes.T_SHORT: {
                type = PrimitiveType.SHORT;
                break;
            }

            case Opcodes.T_INT: {
                type = PrimitiveType.INT;
                break;
            }

            case Opcodes.T_LONG: {
                type = PrimitiveType.INT;
                break;
            }

            default: {
                throw new AssertionError("operand " + operand);
            }
        }

        return type;
    }
}
