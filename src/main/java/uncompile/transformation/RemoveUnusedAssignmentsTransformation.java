package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.*;

public class RemoveUnusedAssignmentsTransformation implements Transformation {
    @Override
    public void run(AstNode node) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                if (method.body != null) {
                    run(method); // TODO: may need more passes
                }
            }
        }.visit(node);
    }

    private void run(Method method) {
        // Find used variables
        Set<VariableDeclaration> usedVariables = new HashSet<>();
        new AstVisitor() {
            @Override
            public void visit(Assignment assignment) {
                if (!(assignment.left instanceof VariableReference)) {
                    visit(assignment.left);
                } else if (hasSideEffectsButNotStandaloneExpression(assignment.right)) {
                    usedVariables.add(((VariableReference) assignment.left).declaration);
                }
                visit(assignment.right);
            }

            @Override
            public void visit(VariableReference variableReference) {
                super.visit(variableReference);
                usedVariables.add(variableReference.declaration);
            }
        }.visit(method.body);

        // Remove unused assignments and declarations
        Map<Expression, Optional<Expression>> substitutions = new HashMap<>();
        new AstVisitor() {
            @Override
            public void visit(Assignment assignment) {
                if (assignment.left instanceof VariableReference && !usedVariables.contains(((VariableReference) assignment.left).declaration)) {
                    substitutions.put(assignment, hasSideEffects(assignment.right) ? Optional.of(assignment.right) : Optional.empty());
                }
            }

            @Override
            public void visit(VariableDeclaration variableDeclaration) {
                if (!usedVariables.contains(variableDeclaration)) {
                    substitutions.put(variableDeclaration, Optional.empty());
                }
            }
        }.visit(method.body);
        AstUtil.substitute(method.body, substitutions);
    }

    // TODO: implement better removal for side-effect-less expressions
    private boolean hasSideEffectsButNotStandaloneExpression(Expression expression) {
        return expression instanceof InstanceFieldReference || // causes class loading, throws illegal access
               expression instanceof StaticFieldReference || // causes class loading, throws illegal access
               expression instanceof ArrayElement || // throws array index out of bounds
               expression instanceof ArrayConstructor; // array dimensions may have side effects
        // TODO: add conditional expressions here once they're implemented
    }

    private boolean hasSideEffects(Expression expression) {
        return !(expression instanceof VariableReference ||
                 expression instanceof IntLiteral ||
                 expression instanceof LongLiteral ||
                 expression instanceof FloatLiteral ||
                 expression instanceof DoubleLiteral ||
                 expression instanceof ParenthesizedExpression && hasSideEffects(((ParenthesizedExpression) expression).expression) ||
                 expression instanceof Cast && hasSideEffects(((Cast) expression).expression) ||
                 expression instanceof ThisReference ||
                 expression instanceof SuperReference);
    }
}
