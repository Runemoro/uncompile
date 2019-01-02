package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class UnaryOperation extends Expression {
    public UnaryOperator operator;
    public Expression expression;

    public UnaryOperation(UnaryOperator operator, Expression expression) {
        this.operator = operator;
        this.expression = expression;
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
        w.append(operator.name)
         .append(expression);
    }
}
