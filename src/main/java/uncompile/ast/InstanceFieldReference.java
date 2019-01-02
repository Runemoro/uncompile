package uncompile.ast;

import uncompile.metadata.FieldDescription;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class InstanceFieldReference extends Expression {
    public Expression target;
    public FieldDescription field;

    public InstanceFieldReference(Expression target, FieldDescription field) {
        this.target = target;
        this.field = field;
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
        w.append(field);
    }
}
