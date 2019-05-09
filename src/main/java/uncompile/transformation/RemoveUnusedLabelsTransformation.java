package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;
import uncompile.astbuilder.LabeledStatement;

import java.util.*;

public class RemoveUnusedLabelsTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        // Remove unnecessary break or continue labels
        new AstVisitor() {
            private final Map<String, Statement> labeledStatements = new HashMap<>();
            private final Deque<Statement> loops = new ArrayDeque<>();

            @Override
            public void visit(LabeledStatement labeledStatement) {
                Statement statement = labeledStatement.statement;

                while (statement instanceof LabeledStatement) {
                    statement = ((LabeledStatement) statement).statement;
                }

                labeledStatements.put(labeledStatement.label, statement);

                super.visit(labeledStatement);
            }

            @Override
            public void visit(WhileLoop whileLoop) {
                loops.push(whileLoop);
                super.visit(whileLoop);
                loops.pop();
            }

            @Override
            public void visit(Break breakExpr) {
                if (Objects.equals(labeledStatements.get(breakExpr.label), loops.getFirst())) {
                    breakExpr.label = null;
                }
            }

            @Override
            public void visit(Continue continueExpr) {
                if (Objects.equals(labeledStatements.get(continueExpr.label), loops.getFirst())) {
                    continueExpr.label = null;
                }
            }
        }.visit(node);

        // Find used labels
        Set<String> usedLabels = new HashSet<>();
        new AstVisitor() {
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
        }.visit(node);

        // Remove unused labels
        new ReplacingAstVisitor() {
            @Override
            public void visit(LabeledStatement labeledStatement) {
                super.visit(labeledStatement);
                if (!usedLabels.contains(labeledStatement.label)) {
                    replace(labeledStatement.statement);
                }
            }
        };
    }
}
