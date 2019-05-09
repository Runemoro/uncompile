package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Break extends Statement {
    public String label;

    public Break(String label) {
        this.label = label;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (label != null) {
            w.append("break ")
             .append(label)
             .append(";");
        } else {
            w.append("break;");
        }
    }
}
