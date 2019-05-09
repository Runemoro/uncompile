package uncompile.transformation;

import uncompile.ast.Class;
import uncompile.ast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GenerateConstructorCallsTransform implements Transformation {
    @Override
    public void run(AstNode node) {
        new AstVisitor() {
            @Override
            public void visit(Method method) {
                if (method.body != null) {
                    run(method);
                }
            }
        }.visit(node);
    }

    // TODO: too dependant on javac output
    private void run(Method method) {
        Map<Expression, Optional<Expression>> substitutions = new HashMap<>();
        StaticMethodCall[] lastConstructorCall = {null};
        new AstVisitor() {
            @Override
            public void visit(AstNode node) {
                if (!(node instanceof Assignment) && lastConstructorCall[0] != null) {
                    transformConstructor(lastConstructorCall[0], substitutions);
                    lastConstructorCall[0] = null;
                }

                super.visit(node);
            }

            @Override
            public void visit(Assignment assignment) {
                if (assignment.left instanceof VariableReference && assignment.right instanceof VariableReference) {
                    VariableReference left = (VariableReference) assignment.left;
                    VariableReference right = (VariableReference) assignment.right;

                    if (lastConstructorCall[0] != null && !left.declaration.isSynthetic &&
                        right.declaration == ((VariableReference) lastConstructorCall[0].arguments.get(0)).declaration) {
                        lastConstructorCall[0].arguments.set(0, left);
                        substitutions.put(assignment, Optional.empty());
                        substitutions.put(right, Optional.of(left));
                    }
                }
            }

            @Override
            public void visit(StaticMethodCall staticMethodCall) {
                super.visit(staticMethodCall);

                if (lastConstructorCall[0] != null) {
                    transformConstructor(lastConstructorCall[0], substitutions);
                    lastConstructorCall[0] = null;
                }

                if (staticMethodCall.method.getName().equals("<init>")) {
                    lastConstructorCall[0] = staticMethodCall;
                }
            }
        }.visit(method);

        if (lastConstructorCall[0] != null) {
            transformConstructor(lastConstructorCall[0], substitutions);
            lastConstructorCall[0] = null;
        }

        AstUtil.substitute(method, substitutions);
    }

    private void transformConstructor(StaticMethodCall lastConstructorCall, Map<Expression, Optional<Expression>> substitutions) {
        Expression thisArgument = lastConstructorCall.arguments.get(0);
        ClassCreationExpression constructorCall = new ClassCreationExpression(lastConstructorCall.owner, lastConstructorCall.method);
        substitutions.put(lastConstructorCall, Optional.of(new Assignment(thisArgument, constructorCall)));
        for (int i = 1; i < lastConstructorCall.arguments.size(); i++) {
            constructorCall.arguments.add(lastConstructorCall.arguments.get(i));
        }
    }
}
