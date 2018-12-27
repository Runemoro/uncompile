package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class NullLiteral extends Expression {
    @Override
    public Type getType() {
        return NullType.INSTANCE;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("null");
    }
}