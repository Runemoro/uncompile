package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class ConstructorCall extends Expression {
    public ReferenceTypeNode type;
    public List<Expression> arguments = new ArrayList<>();

    public ConstructorCall(ReferenceTypeNode type) {
        this.type = type;
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
        w.append("new ");
        type.append(w);
        w.append("(");
        boolean first = true;
        for (Expression argument : arguments) {
            if (!first) {
                w.append(", ");
            }
            first = false;
            argument.append(w);
        }
        w.append(")");
    }
}
