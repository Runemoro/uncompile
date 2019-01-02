package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;

import java.util.function.BiFunction;

public enum BinaryOperator {
    EQ("==", (left, right) -> PrimitiveType.BOOLEAN),
    NE("!=", (left, right) -> PrimitiveType.BOOLEAN),
    GT(">", (left, right) -> PrimitiveType.BOOLEAN),
    GE(">=", (left, right) -> PrimitiveType.BOOLEAN),
    LT("<", (left, right) -> PrimitiveType.BOOLEAN),
    LE("<", (left, right) -> PrimitiveType.BOOLEAN),
    AND("&&", (left, right) -> PrimitiveType.BOOLEAN),
    OR("||", (left, right) -> PrimitiveType.BOOLEAN),
    ADD("+", BinaryOperator::getMathOperationType),
    SUBTRACT("-", BinaryOperator::getMathOperationType),
    MULTIPLY("*", BinaryOperator::getMathOperationType),
    DIVIDE("/", BinaryOperator::getMathOperationType),
    REMAINDER("%", BinaryOperator::getMathOperationType),
    LEFT_SHIFT("<<", BinaryOperator::getMathOperationType),
    RIGHT_SHIFT(">>", BinaryOperator::getMathOperationType),
    UNSIGNED_RIGHT_SHIFT(">>>", BinaryOperator::getMathOperationType),
    BITWISE_AND("&", BinaryOperator::getMathOperationType),
    BITWISE_OR("|", BinaryOperator::getMathOperationType),
    BITWISE_XOR("^", BinaryOperator::getMathOperationType),
    INSTANCEOF("instanceof", (left, right) -> PrimitiveType.BOOLEAN);

    private static Type getMathOperationType(Type left, Type right) {
        return null; // TODO
    }

    public final String name;
    private final BiFunction<Type, Type, Type> typeFunction;

    public Type getType(Type left, Type right) {
        return typeFunction.apply(left, right);
    }

    BinaryOperator(String name, BiFunction<Type, Type, Type> typeFunction) {
        this.name = name;
        this.typeFunction = typeFunction;
    }
}
