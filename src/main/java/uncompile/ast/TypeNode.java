package uncompile.ast;

import uncompile.metadata.ArrayType;
import uncompile.metadata.ClassType;
import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;

public abstract class TypeNode extends AstNode {
    public static TypeNode fromType(Type type) {
        if (type instanceof ArrayType) {
            return new ArrayTypeLiteral(fromType(((ArrayType) type).getComponentType()));
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
}
