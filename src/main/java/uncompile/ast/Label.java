package uncompile.ast;

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

    @Override
    public boolean equals(Object o) {
        return o == this ||
               o instanceof Label &&
               name.equals(((Label) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
