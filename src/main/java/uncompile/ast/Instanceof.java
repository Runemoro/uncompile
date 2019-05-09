package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Instanceof extends Expression {
    public Expression expression;
    public ReferenceTypeNode type;

    public Instanceof(Expression expression, ReferenceTypeNode type) {
        this.expression = expression;
        this.type = type;
    }

    @Override
    public Type getType() {
        return PrimitiveType.BOOLEAN;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(expression)
         .append(" instanceof ")
         .append(type);
    }
}
