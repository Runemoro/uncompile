package uncompile.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TransformingAstVisitor extends AstVisitor {
    public Expression transform(Expression expression) {
        return expression;
    }

    public Block transform(Block block) {
        return block;
    }

    private <T> void transform(List<T> list, Function<T, T> transformation) {
        List<T> old = new ArrayList<>(list);
        list.clear();
        for (T t : old) {
            t = transformation.apply(t);
            if (t != null) {
                list.add(t);
            }
        }
    }

    @Override
    public void visit(ArrayElement arrayElement) {
        super.visit(arrayElement);
        arrayElement.array = transform(arrayElement.array);
        arrayElement.index = transform(arrayElement.index);
    }

    @Override
    public void visit(Assignment assignment) {
        super.visit(assignment);
        assignment.left = transform(assignment.left);
        assignment.right = transform(assignment.right);
    }

    @Override
    public void visit(BinaryOperation binaryOperation) {
        super.visit(binaryOperation);
        binaryOperation.left = transform(binaryOperation.left);
        binaryOperation.right = transform(binaryOperation.right);
    }

    @Override
    public void visit(Block block) {
        transform(block.expressions, this::transform);
        super.visit(block);
    }

    @Override
    public void visit(Cast cast) {
        cast.expression = transform(cast.expression);
        super.visit(cast);
    }

    @Override
    public void visit(ArrayLength arrayLength) {
        arrayLength.array = transform(arrayLength.array);
        super.visit(arrayLength);
    }

    @Override
    public void visit(ConstructorCall constructorCall) {
        transform(constructorCall.arguments, this::transform);
        super.visit(constructorCall);
    }

    @Override
    public void visit(Field field) {
        field.initialValue = transform(field.initialValue);
        super.visit(field);
    }

    @Override
    public void visit(Goto gotoExpr) {
        gotoExpr.condition = transform(gotoExpr.condition);
        super.visit(gotoExpr);
    }

    @Override
    public void visit(If ifExpr) {
        ifExpr.condition = transform(ifExpr.condition);
        ifExpr.ifBlock = transform(ifExpr.ifBlock);
        super.visit(ifExpr);
    }

    @Override
    public void visit(InstanceFieldReference instanceFieldReference) {
        instanceFieldReference.target = transform(instanceFieldReference.target);
        super.visit(instanceFieldReference);
    }

    @Override
    public void visit(InstanceMethodCall instanceMethodCall) {
        instanceMethodCall.target = transform(instanceMethodCall.target);
        transform(instanceMethodCall.arguments, this::transform);
        super.visit(instanceMethodCall);
    }

    @Override
    public void visit(Method method) {
        method.body = transform(method.body);
        super.visit(method);
    }

    @Override
    public void visit(Par par) {
        par.expression = transform(par.expression);
        super.visit(par);
    }

    @Override
    public void visit(Return returnExpr) {
        returnExpr.value = transform(returnExpr.value);
        super.visit(returnExpr);
    }

    @Override
    public void visit(StaticMethodCall staticMethodCall) {
        transform(staticMethodCall.arguments, this::transform);
        super.visit(staticMethodCall);
    }

    @Override
    public void visit(Throw throwExpr) {
        throwExpr.exception = transform(throwExpr.exception);
        super.visit(throwExpr);
    }

    @Override
    public void visit(UnaryOperation unaryOperation) {
        unaryOperation.expression = transform(unaryOperation.expression);
        super.visit(unaryOperation);
    }

    @Override
    public void visit(WhileLoop whileLoop) {
        whileLoop.condition = transform(whileLoop.condition);
        whileLoop.body = transform(whileLoop.body);
        super.visit(whileLoop);
    }
}
