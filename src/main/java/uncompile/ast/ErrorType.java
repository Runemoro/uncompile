package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ErrorType extends Type {
    public static final Type INSTANCE = new ErrorType();


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("Object /* (type inference failed) */");
    }
}
