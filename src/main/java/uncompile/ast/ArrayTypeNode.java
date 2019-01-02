package uncompile.ast;

import uncompile.metadata.ArrayType;
import uncompile.metadata.ClassType;
import uncompile.util.IndentingPrintWriter;

public class ArrayTypeNode extends ClassReference {
    public TypeNode componenentType;

    public ArrayTypeNode(TypeNode componenentType) {
        super(new ClassType(componenentType.getType() + "[]")); // TODO
        this.componenentType = componenentType;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(componenentType)
         .append("[]");
    }

    @Override
    public ArrayType toType() {
        return new ArrayType(componenentType.toType());
    }
}
