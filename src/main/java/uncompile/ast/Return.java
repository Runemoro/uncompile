package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import javax.annotation.Nullable;

public class Return extends Statement {
    @Nullable public Expression value;

    public Return(@Nullable Expression value) {
        this.value = value;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (value == null) {
            w.append("return;");
        } else {
            w.append("return ");
            value.append(w);
            w.append(";");
        }
    }
}
