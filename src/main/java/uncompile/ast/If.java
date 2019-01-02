package uncompile.ast;

import uncompile.metadata.PrimitiveType;
import uncompile.metadata.Type;
import uncompile.util.IndentingPrintWriter;

public class If extends Expression {
    public Expression condition;
    public Block ifBlock;
    public Block elseBlock;

    public If(Expression condition, Block ifBlock, Block elseBlock) {
        this.condition = condition;
        this.ifBlock = ifBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public boolean needsSemicolon() {
        return false;
    }

    @Override
    public Type getType() {
        return PrimitiveType.VOID;
    }


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void append(IndentingPrintWriter w) {
        w.append("if (")
         .append(condition)
         .append(") ")
         .append(ifBlock);

        if (elseBlock != null) {
            w.append(" else ")
             .append(elseBlock);
        }
    }
}
