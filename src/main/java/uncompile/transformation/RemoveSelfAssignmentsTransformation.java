package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

public class RemoveSelfAssignmentsTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        new ReplacingAstVisitor() {
            @Override
            public void visit(Assignment assignment) {
                super.visit(assignment);

                VariableDeclaration leftVariable = assignment.left instanceof VariableReference ? ((VariableReference) assignment.left).declaration : null;
                VariableDeclaration rightVariable = assignment.right instanceof VariableReference ? ((VariableReference) assignment.right).declaration : null;

                if (leftVariable == rightVariable) {
                    replace(assignment.left);
                }
            }

            @Override
            public void visit(ExpressionStatement expressionStatement) {
                super.visit(expressionStatement);

                if (expressionStatement.expression instanceof VariableReference && !(expressionStatement.expression instanceof VariableDeclaration)) {
                    remove();
                }
            }
        }.visit(node);
    }
}
