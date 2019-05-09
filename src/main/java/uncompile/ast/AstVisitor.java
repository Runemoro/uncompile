package uncompile.ast;

import uncompile.astbuilder.LabeledStatement;
import uncompile.metadata.ClassType;
import uncompile.metadata.NullType;

public class AstVisitor {
    public void visit(AstNode node) {
        node.accept(this);
    }

    public final void visit(Iterable<? extends AstNode> nodes) {
        for (AstNode node : nodes) {
            visit(node);
        }
    }

    public void visit(ArrayConstructor arrayConstructor) {
        visit(arrayConstructor.componentType);
        for (Expression expression : arrayConstructor.dimensions) {
            visit(expression);
        }
    }

    public void visit(ArrayElement arrayElement) {
        visit(arrayElement.array);
        visit(arrayElement.index);
    }

    public void visit(Assignment assignment) {
        visit(assignment.left);
        visit(assignment.right);
    }

    public void visit(BinaryOperation binaryOperation) {
        visit(binaryOperation.left);
        visit(binaryOperation.right);
    }

    public void visit(Block block) {
        visit(block.statements);
    }

    public void visit(Cast cast) {
        visit(cast.type);
        visit(cast.expression);
    }

    public void visit(CharLiteral charLiteral) {

    }

    public void visit(Class clazz) {
        visit(clazz.superType);
        visit(clazz.interfaces);
        visit(clazz.innerClasses);
        visit(clazz.fields);
        visit(clazz.methods);
    }

    public void visit(ClassLiteral classLiteral) {
        visit(classLiteral.value);
    }

    public void visit(ArrayLength arrayLength) {
        visit(arrayLength.array);
    }

    public void visit(ArrayTypeLiteral arrayType) {
        visit(arrayType.componenentType);
    }

    public void visit(BooleanLiteral booleanLiteral) {

    }

    public void visit(Break breakExpr) {

    }

    public void visit(ClassReference classReference) {

    }

    public void visit(ClassType classType) {

    }

    public void visit(ClassCreationExpression constructorCall) {
        visit(constructorCall.type);
        visit(constructorCall.arguments);
    }

    public void visit(Continue continueExpr) {

    }

    public void visit(DoubleLiteral doubleLiteral) {

    }

    public void visit(Field field) {
        visit(field.type);
        visit(field.initialValue);
    }

    public void visit(FloatLiteral floatLiteral) {

    }

    public void visit(If ifExpr) {
        visit(ifExpr.condition);
        visit(ifExpr.ifBlock);
        if (ifExpr.elseBlock != null) {
            visit(ifExpr.elseBlock);
        }
    }

    public void visit(InstanceFieldReference instanceFieldReference) {
        visit(instanceFieldReference.target);
    }

    public void visit(InstanceMethodCall instanceMethodCall) {
        visit(instanceMethodCall.target);
        visit(instanceMethodCall.typeArguments);
        visit(instanceMethodCall.arguments);
    }

    public void visit(IntLiteral intLiteral) {

    }

    public void visit(LongLiteral longLiteral) {

    }

    public void visit(Method method) {
        visit(method.typeParameters);
        visit(method.returnType);
        visit(method.parameters);
        visit(method.exceptions);
        if (method.body != null) {
            visit(method.body);
        }
    }

    public void visit(NullLiteral nullLiteral) {

    }

    public void visit(NullType nullType) {

    }

    public void visit(PackageReference packageReference) {

    }

    public void visit(ParenthesizedExpression par) {
        visit(par.expression);
    }

    public void visit(PrimitiveTypeNode primitiveTypeNode) {

    }

    public void visit(Return returnExpr) {
        if (returnExpr.value != null) {
            visit(returnExpr.value);
        }
    }

    public void visit(StaticFieldReference staticFieldReference) {
        visit(staticFieldReference.owner);
    }

    public void visit(StaticMethodCall staticMethodCall) {
        visit(staticMethodCall.owner);
        visit(staticMethodCall.typeArguments);
        visit(staticMethodCall.arguments);
    }

    public void visit(StringLiteral stringLiteral) {

    }

    public void visit(SuperConstructorCall superConstructorCall) {
        visit(superConstructorCall.owner);
        visit(superConstructorCall.arguments);
    }

    public void visit(SuperReference superReference) {
        visit(superReference.owner);
    }

    public void visit(Switch switchExpr) {
        visit(switchExpr.expression);

        for (Expression caseExpr : switchExpr.cases) {
            if (caseExpr != null) {
                visit(caseExpr);
            }
        }

        for (Block branch : switchExpr.branches) {
            visit(branch);
        }
    }

    public void visit(ThisConstructorCall thisConstructorCall) {
        visit(thisConstructorCall.owner);
        visit(thisConstructorCall.arguments);
    }

    public void visit(ThisReference thisReference) {
        visit(thisReference.owner);
    }

    public void visit(Throw throwExpr) {
        visit(throwExpr.exception);
    }

    public void visit(TryCatch tryCatch) {
        visit(tryCatch.resources);
        visit(tryCatch.tryBlock);
        for (TryCatch.Catch catchBlock : tryCatch.catchBlocks) {
            visit(catchBlock.exceptionVariable);
            visit(catchBlock.block);
            visit(catchBlock.exceptionTypes);
        }
        visit(tryCatch.finallyBlock);
    }

    public void visit(TypeParameter typeParameter) {
        if (typeParameter.extendsBound != null) {
            visit(typeParameter.extendsBound);
        }
    }

    public void visit(UnaryOperation unaryOperation) {
        visit(unaryOperation.expression);
    }

    public void visit(VariableDeclaration variableDeclaration) {
        visit(variableDeclaration.type);
    }

    public void visit(VariableReference variableReference) { // (which is not a VariableDeclaration)

    }

    public void visit(Instanceof instanceofExpression) {
        visit(instanceofExpression.expression);
        visit(instanceofExpression.type);
    }

    public void visit(WhileLoop whileLoop) {
        visit(whileLoop.condition);
        visit(whileLoop.body);
    }

    public void visit(Wildcard wildcard) {
        if (wildcard.extendsBound != null) {
            visit(wildcard.extendsBound);
        }

        if (wildcard.superBound != null) {
            visit(wildcard.superBound);
        }
    }


    public void visit(ExpressionStatement expressionStatement) {
        visit(expressionStatement.expression);
    }

    public void visit(LabeledStatement labeledStatement) {
        visit(labeledStatement.statement);
    }
}
