package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class DoubleLiteral extends Expression {
    public double value;

    public DoubleLiteral(double value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return PrimitiveType.DOUBLE;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(String.valueOf(value)).append("D");
    }
}
