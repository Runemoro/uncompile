package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ArrayElement extends Expression {
    public Expression array;
    public Expression index;

    public ArrayElement(Expression array, Expression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public Type getType() {
        if (!(array.getType() instanceof ArrayType)) {
            return ErrorType.INSTANCE;
        }

        return ((ArrayType) array.getType()).elementType;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(array)
         .append("[")
         .append(index)
         .append("]");
    }
}
