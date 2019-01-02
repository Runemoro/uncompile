package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

public class Switch extends Expression {
    public Expression expression;
    public Expression[] cases; // null = default
    public Block[] branches;

    public Switch(Expression expression, Expression[] cases, Block[] branches) {
        this.expression = expression;
        this.cases = cases;
        this.branches = branches;
    }

    @Override
    public Type getType() {
        return PrimitiveType.VOID;
    }

    @Override
    public boolean needsSemicolon() {
        return false;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("switch (")
         .append(expression)
         .append(") {")
         .indent()
         .println();

        for (int i = 0; i < cases.length; i++) {
            if (cases[i] != null) {
                w.append("case ")
                 .append(cases[i])
                 .append(":");
            } else {
                w.append("default:");
            }

            if (branches[i].expressions.isEmpty()) {
                w.println();
            } else {
                w.append(" ")
                 .append(branches[i])
                 .println();

                if (i != branches.length - 1) {
                    w.println();
                }
            }
        }

        w.unindent()
         .append("}");
    }
}
