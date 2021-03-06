package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Throw extends Statement {
    public Expression exception;

    public Throw(Expression exception) {
        this.exception = exception;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("throw ")
         .append(exception)
         .append(";");
    }
}
