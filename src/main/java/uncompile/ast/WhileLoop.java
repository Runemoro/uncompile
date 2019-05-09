package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class WhileLoop extends Statement {
    public Expression condition;
    public Block body;
    public boolean postcondition = false;

    public WhileLoop(Expression condition, Block body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (!postcondition) {
            w.append("while (")
             .append(condition)
             .append(") ")
             .append(body);
        } else {
            w.append("do")
             .append(body)
             .append(" while (")
             .append(condition)
             .append(");");
        }
    }
}
