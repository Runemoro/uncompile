package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class IntLiteral extends Expression {
    public int value;

    public IntLiteral(int value) {
        this.value = value;
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
        w.append(String.valueOf(value));
    }
}
