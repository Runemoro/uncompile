package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Cast extends Expression {
    public Expression expression;
    public Type type;

    public Cast(Expression expression, Type type) {
        this.expression = expression;
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("(");
        type.append(w);
        w.append(") ");
        expression.append(w);
    }
}
