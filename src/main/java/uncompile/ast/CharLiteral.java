package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class CharLiteral extends Expression {
    public char value;

    public CharLiteral(char value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return PrimitiveType.CHAR;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("'");

        if (value == '\'' || value == '\\') {
            w.append("\\");
        }
        w.append(value);

        w.append("'");
    }
}
