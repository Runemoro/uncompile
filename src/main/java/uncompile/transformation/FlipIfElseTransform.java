package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

public class FlipIfElseTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(If ifExpr) {
                super.visit(ifExpr);
                if (ifExpr.elseBlock != null) {
                    boolean ifEmpty = ifExpr.ifBlock.expressions.isEmpty();
                    boolean elseEmpty = ifExpr.elseBlock.expressions.isEmpty();

                    if (elseEmpty) {
                        ifExpr.elseBlock = null;
                    } else if (ifEmpty) {
                        ifExpr.condition = AstUtil.negate(ifExpr.condition);
                        ifExpr.ifBlock = ifExpr.elseBlock;
                        ifExpr.elseBlock = null;
                    } else if (ifExpr.ifBlock.expressions.size() == 1 && ifExpr.ifBlock.expressions.get(0) instanceof If &&
                               (ifExpr.elseBlock.expressions.size() != 1 || !(ifExpr.elseBlock.expressions.get(0) instanceof If))) {
                        ifExpr.condition = AstUtil.negate(ifExpr.condition);
                        Block ifBlock = ifExpr.ifBlock;
                        ifExpr.ifBlock = ifExpr.elseBlock;
                        ifExpr.elseBlock = ifBlock;
                    }
                }
            }
        }.visit(clazz);
    }
}
