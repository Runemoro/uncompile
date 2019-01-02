package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class ThisConstructorCall extends Expression {
    public ThisReference owner;
    public List<Expression> arguments = new ArrayList<>();

    public ThisConstructorCall(ThisReference owner) {
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
