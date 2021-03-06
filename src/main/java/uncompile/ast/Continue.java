package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Continue extends Expression {
    public String label;

    public Continue(String label) {
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
             .append(label);
        } else {
            w.append("continue");
        }
    }
}
