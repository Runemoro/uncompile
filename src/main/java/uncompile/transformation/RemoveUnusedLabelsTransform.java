package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.HashSet;
import java.util.Set;

public class RemoveUnusedLabelsTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        Set<Label> usedLabels = new HashSet<>();
        new AstVisitor() {
            @Override
            public void visit(Goto gotoExpr) {
                super.visit(gotoExpr);
                usedLabels.add(gotoExpr.target);
            }
        }.visit(clazz);

        new TransformingAstVisitor() {
            @Override
            public Expression transform(Expression expression) {
                if (expression instanceof Label && !usedLabels.contains(expression)) {
                    return null;
                }
                return expression;
            }
        }.visit(clazz);
        System.out.println(clazz);
        System.out.println();
    }
}
