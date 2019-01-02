package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Block extends Expression implements Iterable<Expression> {
    public List<Expression> expressions = new ArrayList<>();

    public void add(Expression expression) {
        expressions.add(expression);
    }

    @Override
    public Iterator<Expression> iterator() {
        return expressions.iterator();
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
        if (expressions.isEmpty()) {
            w.append("{}");
        } else {
            w.append("{");
            w.indent();
            w.println();
            for (Expression expression : expressions) {
                expression.append(w);
                if (expression.needsSemicolon()) {
                    w.println(";");
                } else {
                    w.println();
                }
            }
            w.unindent();
            w.print("}");
        }
    }
}
