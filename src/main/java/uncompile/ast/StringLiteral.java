package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class StringLiteral extends Expression {
    public String value;

    public StringLiteral(String value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return ClassType.STRING;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("\"");

        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') {
                w.append("\\");
            }
            w.append(c);
        }

        w.append("\"");
    }
}
