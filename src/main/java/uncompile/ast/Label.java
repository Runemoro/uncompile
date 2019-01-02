package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Label extends Expression {
    public String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public boolean needsSemicolon() {
        return false;
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
        w.append(name)
         .append(":");
    }
}
