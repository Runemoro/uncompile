package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class NewInstance extends Expression {
    public ClassReference type;

    public NewInstance(ClassReference type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("null /* (uninitialized instance of ");
        type.append(w);
        w.append(") */");
    }
}
