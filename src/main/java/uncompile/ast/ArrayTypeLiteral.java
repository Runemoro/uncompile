package uncompile.ast;

import uncompile.metadata.ArrayType;
import uncompile.metadata.ClassType;
import uncompile.util.IndentingPrintWriter;

public class ArrayTypeLiteral extends ClassReference {
    public TypeNode componenentType;

    public ArrayTypeLiteral(TypeNode componenentType) {
        super(new ClassType(componenentType.toType() + "[]")); // TODO
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
