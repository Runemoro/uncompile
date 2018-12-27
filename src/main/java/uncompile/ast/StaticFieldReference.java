package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class StaticFieldReference extends Expression {
    public ClassReference owner;
    public String name;

    public StaticFieldReference(ClassReference owner, String name) {
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
        w.append(name);
    }
}
