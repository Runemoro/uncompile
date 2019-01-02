package uncompile.ast;

import uncompile.metadata.MethodDescription;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class InstanceMethodCall extends Expression { // TODO: MethodDescription, ClassDescription, resolve()
    public Expression target;
    public MethodDescription method;
    public List<ReferenceTypeNode> typeArguments = new ArrayList<>();
    public List<Expression> arguments = new ArrayList<>();

    public InstanceMethodCall(Expression target, MethodDescription method) {
        this.target = target;
        this.method = method;
    }

    @Override
    public Type getType() {
        return null; // TODO
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        target.append(w);
        w.append(".");

        if (!typeArguments.isEmpty()) {
            w.append("<");
            boolean first = true;
            for (ReferenceTypeNode type : typeArguments) {
                if (!first) {
                    w.append(", ");
                }
                first = false;
                type.append(w);
            }
            w.append(">");
        }

        w.append(method.getName());
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
