package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class BooleanLiteral extends Expression{
    public boolean value;

    public BooleanLiteral(boolean value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return PrimitiveType.BOOLEAN;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(value ? "true" : "false");
    }
}
