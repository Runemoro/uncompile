package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

public class MergeNestedIfsTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        new AstVisitor() {
            @Override
            public void visit(If ifExpr) {
                while (ifExpr.elseBlock == null &&
                       ifExpr.ifBlock.statements.size() == 1 &&
                       ifExpr.ifBlock.statements.get(0) instanceof If &&
                       ((If) ifExpr.ifBlock.statements.get(0)).elseBlock == null) {
                    ifExpr.condition = new BinaryOperation(BinaryOperator.AND, new ParenthesizedExpression(ifExpr.condition), new ParenthesizedExpression(((If) ifExpr.ifBlock.statements.get(0)).condition));
                    ifExpr.ifBlock = ((If) ifExpr.ifBlock.statements.get(0)).ifBlock;
                }
            }
        }.visit(node);
    }
}
