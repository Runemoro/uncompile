package uncompile.ast;

public abstract class Type extends Expression {
    @Override
    public Type getType() {
        return PrimitiveType.VOID;
    }
}
