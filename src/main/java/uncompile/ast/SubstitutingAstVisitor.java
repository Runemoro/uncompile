package uncompile.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubstitutingAstVisitor extends AstVisitor { // TODO: make this an extensible visitor
    private final Map<? extends Expression, Optional<Expression>> substitutions;
    private boolean changed = false;

    public SubstitutingAstVisitor(Map<? extends Expression, Optional<Expression>> substitutions) {
        this.substitutions = substitutions;
    }

    public boolean changed() {
        return changed;
    }

    private Expression substitute(Expression expression) {
        Expression substitution = substitutions.getOrDefault(expression, Optional.ofNullable(expression)).orElse(null);
        if (substitution != expression) {
            changed = true;
        }
        return substitution;
    }

    private void substitute(List<Expression> list) {
        List<Expression> old = new ArrayList<>(list);
        list.clear();
        for (Expression t : old) {
            t = substitute(t);
            if (t != null) {
                list.add(t);
            }
        }
    }

    @Override
    public void visit(ArrayConstructor arrayConstructor) {
        for (int i = 0; i < arrayConstructor.dimensions.length; i++) {
            arrayConstructor.dimensions[i] = substitute(arrayConstructor.dimensions[i]);
        }
        super.visit(arrayConstructor);
    }

    @Override
    public void visit(ArrayElement arrayElement) {
        arrayElement.array = substitute(arrayElement.array);
        arrayElement.index = substitute(arrayElement.index);
        super.visit(arrayElement);
    }

    @Override
    public void visit(Assignment assignment) {
        assignment.left = substitute(assignment.left);
        assignment.right = substitute(assignment.right);
        super.visit(assignment);
    }

    @Override
    public void visit(BinaryOperation binaryOperation) {
        binaryOperation.left = substitute(binaryOperation.left);
        binaryOperation.right = substitute(binaryOperation.right);
        super.visit(binaryOperation);
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
    }

    @Override
    public void visit(ExpressionStatement expressionStatement) {
        expressionStatement.expression = substitute(expressionStatement.expression);
    }

    @Override
    public void visit(Cast cast) {
        cast.expression = substitute(cast.expression);
        super.visit(cast);
    }

    @Override
    public void visit(ArrayLength arrayLength) {
        arrayLength.array = substitute(arrayLength.array);
        super.visit(arrayLength);
    }

    @Override
    public void visit(ClassCreationExpression constructorCall) {
        substitute(constructorCall.arguments);
        super.visit(constructorCall);
    }

    @Override
    public void visit(Field field) {
        field.initialValue = substitute(field.initialValue);
        super.visit(field);
    }

    @Override
    public void visit(If ifExpr) {
        ifExpr.condition = substitute(ifExpr.condition);
        super.visit(ifExpr);
    }

    @Override
    public void visit(InstanceFieldReference instanceFieldReference) {
        instanceFieldReference.target = substitute(instanceFieldReference.target);
        super.visit(instanceFieldReference);
    }

    @Override
    public void visit(InstanceMethodCall instanceMethodCall) {
        instanceMethodCall.target = substitute(instanceMethodCall.target);
        substitute(instanceMethodCall.arguments);
        super.visit(instanceMethodCall);
    }

    @Override
    public void visit(ParenthesizedExpression par) {
        par.expression = substitute(par.expression);
        super.visit(par);
    }

    @Override
    public void visit(Return returnExpr) {
        if (returnExpr.value != null) {
            returnExpr.value = substitute(returnExpr.value);
        }
        super.visit(returnExpr);
    }

    @Override
    public void visit(StaticMethodCall staticMethodCall) {
        substitute(staticMethodCall.arguments);
        super.visit(staticMethodCall);
    }

    @Override
    public void visit(SuperConstructorCall superConstructorCall) {
        substitute(superConstructorCall.arguments);
        super.visit(superConstructorCall);
    }

    @Override
    public void visit(Switch switchExpr) {
        switchExpr.expression = substitute(switchExpr.expression);

        for (int i = 0; i < switchExpr.cases.length; i++) {
            switchExpr.cases[i] = substitute(switchExpr.cases[i]);
        }

        super.visit(switchExpr);
    }

    @Override
    public void visit(ThisConstructorCall thisConstructorCall) {
        substitute(thisConstructorCall.arguments);
        super.visit(thisConstructorCall);
    }

    @Override
    public void visit(Throw throwExpr) {
        throwExpr.exception = substitute(throwExpr.exception);
        super.visit(throwExpr);
    }

    @Override
    public void visit(UnaryOperation unaryOperation) {
        unaryOperation.expression = substitute(unaryOperation.expression);
        super.visit(unaryOperation);
    }

    @Override
    public void visit(WhileLoop whileLoop) {
        whileLoop.condition = substitute(whileLoop.condition);
        super.visit(whileLoop);
    }
}
