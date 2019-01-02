package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class ClassLiteral extends Expression {
    public Type value; // not ObjectType, primitives have classes too: int.class

    public ClassLiteral(Type value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return ClassType.CLASS;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(value)
         .append(".class");
    }
}
