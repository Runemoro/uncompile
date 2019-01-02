package uncompile.ast;

import uncompile.metadata.ArrayType;
import uncompile.metadata.Type;
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
        Type arrayType = array.getType();

        if (!(arrayType instanceof ArrayType)) {
            return null;
        }

        return ((ArrayType) arrayType).getComponentType();
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
