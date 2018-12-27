package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class WhileLoop extends Expression {
    public Expression condition;
    public Block body;

    public WhileLoop(Expression condition, Block body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public boolean needsSemicolon() {
        return false;
    }

    @Override
    public Type getType() {
        return PrimitiveType.VOID;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("while (")
         .append(condition)
         .append(") ")
         .append(body);
    }
}
