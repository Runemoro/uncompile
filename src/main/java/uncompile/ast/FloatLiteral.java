package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class FloatLiteral extends Expression {
    public float value;

    public FloatLiteral(float value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return PrimitiveType.FLOAT;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(String.valueOf(value)).append("F");
    }
}
