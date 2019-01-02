package uncompile.ast;

import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class SuperReference extends Expression {
    public ClassReference owner;
    public boolean isQualified;

    public SuperReference(ClassReference owner, boolean isQualified) {
        this.owner = owner;
        this.isQualified = isQualified;
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
        if (isQualified) {
            w.append(owner)
             .append(".super");
        } else {
            w.append("super");
        }
    }
}
