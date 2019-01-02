package uncompile.ast;

import uncompile.metadata.ClassType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class ClassLiteral extends Expression {
    public TypeNode value; // not ReferenceTypeNode, primitives have classes too: int.class

    public ClassLiteral(TypeNode value) {
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
