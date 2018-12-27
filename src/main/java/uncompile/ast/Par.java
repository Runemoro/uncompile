package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Par extends Expression {
    public Expression expression;

    public Par(Expression expression) {
        this.expression = expression;
    }

    @Override
    public Type getType() {
        return expression.getType();
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("(");
        expression.append(w);
        w.append(")");
    }
}
