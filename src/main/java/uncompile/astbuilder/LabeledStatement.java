package uncompile.astbuilder;

import uncompile.ast.AstVisitor;
import uncompile.ast.Statement;
import uncompile.util.IndentingPrintWriter;

public class LabeledStatement extends Statement {
    public String label;
    public Statement statement;

    public LabeledStatement(String label, Statement statement) {
        this.label = label;
        this.statement = statement;
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append(label)
         .append(": ")
         .append(statement);
    }
}
