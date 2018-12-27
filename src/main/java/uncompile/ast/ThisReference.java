package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ThisReference extends VariableReference {
    public ClassType owner;
    public boolean isQualified = true;

    public ThisReference(ClassType owner) {
        super(null);
        this.owner = owner;
    }

    @Override
    public Type getType() {
        return owner;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (isQualified) {
            w.append(owner)
             .append(".");
        }

        w.append("this");
    }
}
