package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Break extends Expression {
    public Label label;

    public Break(Label label) {
        this.label = label;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (label != null) {
            w.append("break ")
             .append(label.name);
        } else {
            w.append("break");
        }
    }
}
