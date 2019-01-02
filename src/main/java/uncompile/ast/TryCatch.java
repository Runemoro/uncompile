package uncompile.ast;

import uncompile.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.List;

public class TryCatch extends Expression {
    public static class Catch extends AstNode { // TODO: 'implements Appendable' instead of AstNode
        public List<Type> exceptionTypes = new ArrayList<>();
        public VariableDeclaration exceptionVariable;
        public Block block;

        public Catch(VariableDeclaration exceptionVariable, Block block) {
            this.exceptionVariable = exceptionVariable;
            this.block = block;
        }

        @Override
        public void accept(AstVisitor visitor) {
            visitor.visit(this); // TODO
        }

        @Override
        public void append(IndentingPrintWriter w) {
            w.append("catch (");

            boolean first = true;
            for (Type exceptionType : exceptionTypes) {
                if (!first) {
                    w.append(" | ");
                }
                w.append(exceptionType);
            }
            w.append(" ")
             .append(exceptionVariable.name);

            w.append(") ")
             .append(block);
        }
    }

    public Block resources = new Block();
    public Block tryBlock;
    public List<Catch> catchBlocks = new ArrayList<>();
    public Block finallyBlock = new Block();

    public TryCatch(Block tryBlock) {
        this.tryBlock = tryBlock;
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
        if (!resources.expressions.isEmpty()) {
            w.append("try (");
            w.indent(5);
            int i = 0;
            for (Expression expression : resources) {
                w.append(expression);
                if (i++ != resources.expressions.size() - 1) {
                    w.append(";");
                    w.println();
                }
            }
            w.unindent(5);
            w.append(") ");
        } else {
            w.append("try ");
        }

        w.append(tryBlock);

        for (Catch catchBlock : catchBlocks) {
            w.append(" ")
             .append(catchBlock);
        }

        if (!finallyBlock.expressions.isEmpty()) {
            w.append(" finally ")
             .append(finallyBlock);
        }
    }
}
