package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.HashSet;
import java.util.Set;

/*
 * Should run before any conditional expression transforms because those are
 * not supported by this transform.
 */
public class RemoveUnusedAssignmentsTransform implements Transformation {
    @Override
    public void run(Class clazz) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                if (method.body != null) {
                    run(method); // TODO: may need more passes
                }
            }
        }.visit(clazz);
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
        new TransformingAstVisitor() {
            @Override
            public Expression transform(Expression expression) {
                if (expression instanceof Assignment) {
                    Assignment assignment = (Assignment) expression;
                    if (assignment.left instanceof VariableReference &&
                        !usedVariables.contains(((VariableReference) assignment.left).declaration)) {
                        return hasSideEffects(assignment.right) ? assignment.right : null;
                    }
                }

                if (expression instanceof VariableDeclaration &&
                    !usedVariables.contains(((VariableDeclaration) expression).declaration)) {
                    return null;
                }

                return expression;
            }
        }.visit(method.body);
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
                 expression instanceof Par && hasSideEffects(((Par) expression).expression) ||
                 expression instanceof Cast && hasSideEffects(((Cast) expression).expression) ||
                 expression instanceof ThisReference ||
                 expression instanceof SuperReference);
    }
}
