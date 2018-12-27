package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class InstanceFieldReference extends Expression {
    public Expression target;
    public String name;

    public InstanceFieldReference(Expression target, String name) {
        this.target = target;
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
        w.append(target);
        w.append(".");
        w.append(name);
    }
}
