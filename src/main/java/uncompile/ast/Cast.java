package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Cast extends Expression {
    public Expression expression;
    public TypeNode type;

    public Cast(Expression expression, TypeNode type) {
        this.expression = expression;
        this.type = type;
    }

    @Override
    public Type getType() {
        return type.toType();
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
