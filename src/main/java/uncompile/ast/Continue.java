package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Continue extends Expression {
    public Label label;

    public Continue(Label label) {
        this.label = label;
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
        if (label != null) {
            w.append("continue ")
             .append(label.name);
        } else {
            w.append("continue");
        }
    }
}
