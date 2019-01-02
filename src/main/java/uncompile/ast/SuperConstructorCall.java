package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class SuperConstructorCall extends Expression {
    public SuperReference owner;
    public List<Expression> arguments = new ArrayList<>();

    public SuperConstructorCall(SuperReference owner) {
        this.owner = owner;
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
        w.append(owner)
         .append("(");

        boolean first = true;
        for (Expression argument : arguments) {
            if (!first) {
                w.append(", ");
            }
            first = false;

            w.append(argument);
        }

        w.append(")");
    }
}
