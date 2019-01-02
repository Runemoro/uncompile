package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;

import java.util.function.Function;

public enum UnaryOperator {
    MINUS("-", type -> type),
    BITWISE_NOT("~", type -> type),
    NOT("!", type -> PrimitiveType.BOOLEAN);

    public final String name;
    private final Function<Type, Type> typeFunction;

    public Type getType(Type exprssion) {
        return typeFunction.apply(exprssion);
    }

    UnaryOperator(String name, Function<Type, Type> typeFunction) {
        this.name = name;
        this.typeFunction = typeFunction;
    }
}
