package uncompile.ast;

import uncompile.metadata.MethodDescription;
import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class ClassCreationExpression extends Expression { // TODO: primary, type arguments
    public ReferenceTypeNode type;
    public MethodDescription method;
    public List<Expression> arguments = new ArrayList<>();

    public ClassCreationExpression(ReferenceTypeNode type, MethodDescription method) {
        this.type = type;
        this.method = method;
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
