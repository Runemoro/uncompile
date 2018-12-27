package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class LongLiteral extends Expression {
    public long value;

    public LongLiteral(long value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return PrimitiveType.LONG;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(String.valueOf(value)).append("L");
    }
}
