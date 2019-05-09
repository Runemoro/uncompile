package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

public class FlipIfElseTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        new AstVisitor() {
            @Override
            public void visit(If ifExpr) {
                super.visit(ifExpr);
                if (ifExpr.elseBlock != null) {
                    boolean ifEmpty = ifExpr.ifBlock.statements.isEmpty();
                    boolean elseEmpty = ifExpr.elseBlock.statements.isEmpty();

                    if (elseEmpty) {
                        ifExpr.elseBlock = null;
                    } else if (ifEmpty) {
                        ifExpr.condition = AstUtil.negate(ifExpr.condition);
                        ifExpr.ifBlock = ifExpr.elseBlock;
                        ifExpr.elseBlock = null;
                    } else if (ifExpr.ifBlock.statements.size() == 1 && ifExpr.ifBlock.statements.get(0) instanceof If &&
                               (ifExpr.elseBlock.statements.size() != 1 || !(ifExpr.elseBlock.statements.get(0) instanceof If))) {
                        ifExpr.condition = AstUtil.negate(ifExpr.condition);
                        Block ifBlock = ifExpr.ifBlock;
                        ifExpr.ifBlock = ifExpr.elseBlock;
                        ifExpr.elseBlock = ifBlock;
                    }
                }
            }
        }.visit(node);
    }
}
