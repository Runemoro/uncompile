package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Return extends Expression {
    public Expression value;

    public Return(Expression value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return PrimitiveType.VOID;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (value == null) {
            w.append("return");
        } else {
            w.append("return ");
            value.append(w);
        }
    }
}
