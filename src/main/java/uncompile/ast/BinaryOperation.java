package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class BinaryOperation extends Expression {
    public BinaryOperator operator;
    public Expression left;
    public Expression right;

    public BinaryOperation(BinaryOperator operator, Expression left, Expression right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(left)
         .append(" ")
         .append(operator.name)
         .append(" ")
         .append(right);
    }
}
