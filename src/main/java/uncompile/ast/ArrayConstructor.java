package uncompile.ast;

import uncompile.metadata.ArrayType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class ArrayConstructor extends Expression {
    public TypeNode componentType;
    public Expression[] dimensions;

    public ArrayConstructor(TypeNode componentType, Expression[] dimensions) {
        this.componentType = componentType;
        this.dimensions = dimensions;
    }

    @Override
    public Type getType() {
        Type type = componentType.toType();
        for (int i = 0; i < dimensions.length; i++) {
            type = new ArrayType(type);
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
         .append(componentType);
        for (Expression dimension : dimensions) {
            w.append("[")
             .append(dimension)
             .append("]");
        }
    }
}
