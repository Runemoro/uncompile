package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Label extends Expression {
    public String name;
    public Block declarationScope;

    public Label(String name, Block declarationScope) {
        this.name = name;
        this.declarationScope = declarationScope;
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
    public boolean equals(Object obj) {
        return obj == this ||
               obj instanceof Label &&
               name.equals(((Label) obj).name) &&
               declarationScope.equals(((Label) obj).declarationScope);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + declarationScope.hashCode();
    }
}
