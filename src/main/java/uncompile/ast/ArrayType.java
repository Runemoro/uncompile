package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ArrayType extends ClassType {
    public Type elementType;

    public ArrayType(Type elementType) {
        super(null); // TODO
        this.elementType = elementType;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        elementType.append(w);
        w.append("[]");
    }
}
