package uncompile.ast;

import uncompile.metadata.FieldDescription;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class StaticFieldReference extends Expression {
    public ClassReference owner;
    public FieldDescription field;

    public StaticFieldReference(ClassReference owner, FieldDescription field) {
        this.owner = owner;
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
        owner.append(w);
        w.append(".");
        w.append(field.getName());
    }
}
