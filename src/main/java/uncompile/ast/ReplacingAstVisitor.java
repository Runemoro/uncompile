package uncompile.ast;

import java.util.*;

public class ReplacingAstVisitor extends AstVisitor {
    private final Map<AstNode, Optional<AstNode>> substitutions = new HashMap<>();
    private Deque<AstNode> current = new ArrayDeque<>();
    private boolean changed = false;

    @Override
    public void visit(AstNode node) {
        current.push(node);
        super.visit(node);
        current.pop();
    }

    protected void replace(AstNode with) {
        substitutions.put(current.getFirst(), Optional.of(with));
    }

    protected void remove() {
        substitutions.put(current.getFirst(), Optional.empty());
    }

    public boolean changed() {
        return changed;
    }

    private <T extends AstNode> T substitute(T expression) {
        AstNode substitution = substitutions.getOrDefault(expression, Optional.ofNullable(expression)).orElse(null);
        if (substitution != expression) {
            changed = true;
        }
        return (T) substitution;
    }

    private <T extends AstNode> void substitute(List<T> list) {
        List<T> old = new ArrayList<>(list);
        list.clear();
        for (T t : old) {
            t = substitute(t);
            if (t != null) {
                list.add(t);
            }
        }
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        super.visit(arrayConstructor);
        for (int i = 0; i < arrayConstructor.dimensions.length; i++) {
            arrayConstructor.dimensions[i] = substitute(arrayConstructor.dimensions[i]);
        }
    }

    @Override
    public void visit(ArrayElement arrayElement) {
        super.visit(arrayElement);
        arrayElement.array = substitute(arrayElement.array);
        arrayElement.index = substitute(arrayElement.index);
    }

    @Override
    public void visit(Assignment assignment) {
        super.visit(assignment);
        assignment.left = substitute(assignment.left);
        assignment.right = substitute(assignment.right);
    }

    @Override
    public void visit(BinaryOperation binaryOperation) {
        super.visit(binaryOperation);
        binaryOperation.left = substitute(binaryOperation.left);
        binaryOperation.right = substitute(binaryOperation.right);
    }

    @Override
    public void visit(Block block) {
        super.visit(block);

        List<Statement> newStatements = new ArrayList<>();
        for (Statement statement : block) {
            if (statement instanceof ExpressionStatement && ((ExpressionStatement) statement).expression == null) {
                continue;
            }

            newStatements.add(statement);
        }

        block.statements = newStatements;
        substitute(block.statements);
    }

    @Override
    public void visit(ExpressionStatement expressionStatement) {
        super.visit(expressionStatement);
        expressionStatement.expression = substitute(expressionStatement.expression);
    }

    @Override
    public void visit(Cast cast) {
        super.visit(cast);
        cast.expression = substitute(cast.expression);
    }

    @Override
    public void visit(ArrayLength arrayLength) {
        super.visit(arrayLength);
        arrayLength.array = substitute(arrayLength.array);
    }

    @Override
    public void visit(ClassCreationExpression constructorCall) {
        super.visit(constructorCall);
        substitute(constructorCall.arguments);
    }

    @Override
    public void visit(Field field) {
        super.visit(field);
        field.initialValue = substitute(field.initialValue);
    }

    @Override
    public void visit(If ifExpr) {
        super.visit(ifExpr);
        ifExpr.condition = substitute(ifExpr.condition);
    }

    @Override
    public void visit(InstanceFieldReference instanceFieldReference) {
        super.visit(instanceFieldReference);
        instanceFieldReference.target = substitute(instanceFieldReference.target);
    }

    @Override
    public void visit(InstanceMethodCall instanceMethodCall) {
        super.visit(instanceMethodCall);
        instanceMethodCall.target = substitute(instanceMethodCall.target);
        substitute(instanceMethodCall.arguments);
    }

    @Override
    public void visit(ParenthesizedExpression par) {
        super.visit(par);
        par.expression = substitute(par.expression);
    }

    @Override
    public void visit(Return returnExpr) {
        super.visit(returnExpr);
        if (returnExpr.value != null) {
            returnExpr.value = substitute(returnExpr.value);
        }
    }

    @Override
    public void visit(StaticMethodCall staticMethodCall) {
        super.visit(staticMethodCall);
        substitute(staticMethodCall.arguments);
    }

    @Override
    public void visit(SuperConstructorCall superConstructorCall) {
        super.visit(superConstructorCall);
        substitute(superConstructorCall.arguments);
    }

    @Override
    public void visit(Switch switchExpr) {
        super.visit(switchExpr);

        switchExpr.expression = substitute(switchExpr.expression);

        for (int i = 0; i < switchExpr.cases.length; i++) {
            switchExpr.cases[i] = substitute(switchExpr.cases[i]);
        }
    }

    @Override
    public void visit(ThisConstructorCall thisConstructorCall) {
        super.visit(thisConstructorCall);
        substitute(thisConstructorCall.arguments);
    }

    @Override
    public void visit(Throw throwExpr) {
        super.visit(throwExpr);
        throwExpr.exception = substitute(throwExpr.exception);
    }

    @Override
    public void visit(UnaryOperation unaryOperation) {
        super.visit(unaryOperation);
        unaryOperation.expression = substitute(unaryOperation.expression);
    }

    @Override
    public void visit(WhileLoop whileLoop) {
        super.visit(whileLoop);
        whileLoop.condition = substitute(whileLoop.condition);
    }
}
