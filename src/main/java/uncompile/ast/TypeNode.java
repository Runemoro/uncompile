package uncompile.ast;

import uncompile.metadata.*;

public abstract class TypeNode extends Expression {
    public static TypeNode fromType(Type type) {
        if (type instanceof ArrayType) {
            return new ArrayTypeNode(fromType(((ArrayType) type).getComponentType()));
        }

        if (type instanceof ClassType) {
            return new ClassReference((ClassType) type);
        }

        if (type instanceof PrimitiveType) {
            return new PrimitiveTypeNode((PrimitiveType) type);
        }

        return null;
    }

    public abstract Type toType();

    @Override
    public Type getType() {
        return PrimitiveType.VOID;
    }
}
