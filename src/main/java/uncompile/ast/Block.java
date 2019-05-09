package uncompile.ast;

import uncompile.astbuilder.LabeledStatement;
import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Block extends Statement implements Iterable<Statement> {
    public List<Statement> statements = new ArrayList<>();

    public void add(Statement statement) {
        statements.add(statement);
    }

    public void add(Expression expression) {
        statements.add(new ExpressionStatement(expression));
    }

    public void addStatements(Iterable<? extends Statement> statements) {
        for (Statement statement : statements) {
            add(statement);
        }
    }

    public void addExpressions(Iterable<? extends Expression> expressions) {
        for (Expression expression : expressions) {
            add(expression);
        }
    }

    @Override
    public Iterator<Statement> iterator() {
        return statements.iterator();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        if (statements.isEmpty()) {
            w.append("{}");
        } else {
            w.append("{");
            w.indent();
            w.println();
            boolean first = true;
            boolean lastWasBlock = false;
            for (Statement statement : statements) {
                Statement innerStatement = statement;
                while (innerStatement instanceof LabeledStatement) {
                    innerStatement = ((LabeledStatement) innerStatement).statement;
                }
                if (!first && innerStatement instanceof Block || innerStatement instanceof WhileLoop || innerStatement instanceof If) {
                    w.println();
                    lastWasBlock = true;
                } else {
                    lastWasBlock = false;
                }

                first = false;
                statement.append(w);
                w.println();
            }
            w.unindent();
            w.print("}");
        }
    }
}
