package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class StaticMethodCall extends Expression {
    public ClassReference owner;
    public String name;
    public List<ObjectType> typeArguments = new ArrayList<>();
    public List<Expression> arguments = new ArrayList<>();

    public StaticMethodCall(ClassReference owner, String name) {
        this.owner = owner;
        this.name = name;
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
        owner.append(w);
        w.append(".");

        if (!typeArguments.isEmpty()) {
            w.append("<");
            boolean first = true;
            for (ObjectType type : typeArguments) {
                if (!first) {
                    w.append(", ");
                }
                first = false;
                type.append(w);
            }
            w.append(">");
        }

        w.append(name);
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
