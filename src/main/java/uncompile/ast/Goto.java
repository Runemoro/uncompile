package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class Goto extends Expression {
    public Label target;
    public Expression condition;

    public Goto(Label target, Expression condition) {
        this.target = target;
        this.condition = condition;
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
        if (condition == null) {
            w.append("goto(")
             .append(target.name)
             .append(")");
        } else {
            w.append("goto(")
             .append(target.name)
             .append(", ");
             condition.append(w);
             w.append(")");
        }
    }
}
