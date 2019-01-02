package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * - Removes unnecessary blocks
 * - Removes unreachable statements that follow continue, break, or return
 * - Removes unnecessary continue, break or return statements
 * - Removes unnecessary labels for continue, break or return statements
 * - Flips if-else blocks with an empty if, removes empty else blocks
 */
public class SimplifyControlFlowTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                super.visit(method);
                run(method);
            }
        }.visit(clazz);
    }

    private void run(Method method) {
        // Remove unnecessary blocks
        new AstVisitor() {
            @Override
            public void visit(Block block) {
                super.visit(block);

                List<Expression> newExpressions = new ArrayList<>();
                for (Expression expression : block) {
                    if (expression instanceof Block) {
                        for (Expression innerExpression : (Block) expression) {
                            newExpressions.add(innerExpression);
                        }
                        continue;
                    }

                    newExpressions.add(expression);
                }
                block.expressions = newExpressions;
            }
        }.visit(method.body);

        // Simplify loops that don't loop and remove unreachable statements
        Set<Expression> redundant = new HashSet<>();
        new AstVisitor() {

        }.visit(method.body);

        // Flip if-else with empty else (should this be its own tranform?)
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
                        ifExpr.condition = new UnaryOperation(UnaryOperator.NOT, new Par(ifExpr.condition));
                        ifExpr.ifBlock = ifExpr.elseBlock;
                        ifExpr.elseBlock = null;
                    }
                }
            }
        }.visit(method.body);
    }
}
