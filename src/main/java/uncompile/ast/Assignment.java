package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Assignment extends Expression {
    public Expression left;
    public Expression right;

    public Assignment(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Type getType() {
        return left.getType();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(left)
         .append(" = ")
         .append(right);
    }
}
