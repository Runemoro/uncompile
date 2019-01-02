package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.HashSet;
import java.util.Set;

public class RemoveUnusedLabelsTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        // Remove unnecessary break or continue labels
        new AstVisitor() {
            public Label currentLoopLabel = null;

            @Override
            public void visit(Block block) {
                Label lastLabel = null;
                for (Expression expression : block) {
                    if (expression instanceof WhileLoop) { // TODO: for and foreach loops too once implemented
                        Label outerLoopLabel = currentLoopLabel;
                        currentLoopLabel = lastLabel;
                        visit(expression);
                        currentLoopLabel = outerLoopLabel;
                    } else {
                        visit(expression);
                    }

                    lastLabel = expression instanceof Label ? (Label) expression : null;
                }
            }

            @Override
            public void visit(Break breakExpr) {
                if (breakExpr.label == currentLoopLabel) {
                    breakExpr.label = null;
                }
            }

            @Override
            public void visit(Continue continueExpr) {
                if (continueExpr.label == currentLoopLabel) {
                    continueExpr.label = null;
                }
            }
        }.visit(clazz);

        // Remove unused labels in code
        Set<Label> usedLabels = new HashSet<>();
        new AstVisitor() {
            @Override
            public void visit(Goto gotoExpr) {
                super.visit(gotoExpr);
                usedLabels.add(gotoExpr.target);
            }

            @Override
            public void visit(Break breakExpr) {
                super.visit(breakExpr);
                usedLabels.add(breakExpr.label);
            }

            @Override
            public void visit(Continue continueExpr) {
                super.visit(continueExpr);
                usedLabels.add(continueExpr.label);
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
    }
}
