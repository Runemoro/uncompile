package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ArrayLength extends Expression {
    public Expression array;

    public ArrayLength(Expression array) {
        this.array = array;
    }

    @Override
    public Type getType() {
        return PrimitiveType.INT;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(array)
         .append(".length");
    }
}
