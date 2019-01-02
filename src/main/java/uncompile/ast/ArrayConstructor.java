package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ArrayConstructor extends Expression {
    public Type elementType;
    public Expression[] dimensions;

    public ArrayConstructor(Type elementType, Expression[] dimensions) {
        this.elementType = elementType;
        this.dimensions = dimensions;
    }

    @Override
    public Type getType() {
        Type type = elementType;
        for (int i = 0; i < dimensions.length; i++) {
            type = new ArrayType(elementType);
        }
        return type;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("new ")
         .append(elementType);
        for (Expression dimension : dimensions) {
            w.append("[")
             .append(dimension)
             .append("]");
        }
    }
}
