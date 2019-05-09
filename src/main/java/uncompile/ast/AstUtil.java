package uncompile.ast;

import uncompile.metadata.PrimitiveType;

import java.util.Map;
import java.util.Optional;

public class AstUtil {
    public static Expression negate(Expression expression) {
        if (expression instanceof ParenthesizedExpression) {
            ParenthesizedExpression par = (ParenthesizedExpression) expression;
            ((ParenthesizedExpression) expression).expression = negate(par.expression);
            return par;
        }

        if (expression instanceof UnaryOperation && ((UnaryOperation) expression).operator == UnaryOperator.NOT) {
            return ((UnaryOperation) expression).expression;
        }

        if (expression instanceof BooleanLiteral) {
            BooleanLiteral booleanLiteral = (BooleanLiteral) expression;
            booleanLiteral.value = !booleanLiteral.value;
            return booleanLiteral;
        }

        if (expression instanceof IntLiteral) {
            IntLiteral intLiteral = (IntLiteral) expression;
            if (intLiteral.value == 0) {
                intLiteral.value = 1;
                return intLiteral;
            }

            if (intLiteral.value == 1) {
                intLiteral.value = 0;
                return intLiteral;
            }
        }

        if (expression instanceof BinaryOperation) {
            BinaryOperation op = (BinaryOperation) expression;
            if (op.left.getType() == PrimitiveType.FLOAT || op.right.getType() == PrimitiveType.FLOAT ||
                op.left.getType() == PrimitiveType.DOUBLE || op.right.getType() == PrimitiveType.DOUBLE) {
                return new UnaryOperation(UnaryOperator.NOT, new ParenthesizedExpression(expression));
            }

            switch (op.operator) {
                case EQ: {
                    op.operator = BinaryOperator.NE;
                    return op;
                }

                case NE: {
                    op.operator = BinaryOperator.EQ;
                    return op;
                }

                case GT: {
                    op.operator = BinaryOperator.LE;
                    return op;
                }

                case LE: {
                    op.operator = BinaryOperator.GT;
                    return op;
                }

                case GE: {
                    op.operator = BinaryOperator.LT;
                    return op;
                }

                case LT: {
                    op.operator = BinaryOperator.GE;
                    return op;
                }

                case AND: {
                    op.operator = BinaryOperator.OR;
                    return op;
                }

                case OR: {
                    op.operator = BinaryOperator.AND;
                    return op;
                }
            }
        }

        return new UnaryOperation(UnaryOperator.NOT, new ParenthesizedExpression(expression));
    }

    public static boolean substitute(AstNode expression, Map<? extends Expression, Optional<Expression>> substitutions) {
        SubstitutingAstVisitor visitor = new SubstitutingAstVisitor(substitutions);
        visitor.visit(expression);
        return visitor.changed();
    }
}
