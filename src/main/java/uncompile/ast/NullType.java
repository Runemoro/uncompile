package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class NullType extends Type {
    public static final Type INSTANCE = new NullType();

    private NullType() {}


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("NullType /* (error) */");
    }
}
