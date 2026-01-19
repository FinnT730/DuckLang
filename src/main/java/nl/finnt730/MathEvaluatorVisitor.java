package nl.finnt730;

import java.util.HashMap;
import java.util.Map;

public final class MathEvaluatorVisitor extends DLangBaseVisitor<Object> {

    private final Map<String, Integer> variables = new HashMap<>();

    @Override
    public Object visitCompileUnit(DLangParser.CompileUnitContext ctx) {
        return super.visitCompileUnit(ctx);
    }

    @Override
    public Object visitPrintStatement(DLangParser.PrintStatementContext ctx) {
        Object value = visit(ctx.expression());
        System.out.println(value);
        return null;
    }

    @Override
    public Object visitVariableStatement(DLangParser.VariableStatementContext ctx) {
        String id = ctx.ID().getText();
        Object value = visit(ctx.expression());
        if (value instanceof Integer) {
            variables.put(id, (Integer) value);
        }
        return null;
    }

    @Override
    public Object visitExpressionStatement(DLangParser.ExpressionStatementContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Object visitCondition(DLangParser.ConditionContext ctx) {
        Object leftObj = visit(ctx.expression(0));
        Object rightObj = visit(ctx.expression(1));

        if (leftObj instanceof Integer && rightObj instanceof Integer) {
            int left = (Integer) leftObj;
            int right = (Integer) rightObj;

            if (ctx.operator.getType() == DLangParser.GRT) {
                return left > right;
            } else if (ctx.operator.getType() == DLangParser.LST) {
                return left < right;
            }
        }
        return false;
    }

    @Override
    public Object visitIfStatement(DLangParser.IfStatementContext ctx) {
        Object conditionResult = visit(ctx.condition());

        if (Boolean.TRUE.equals(conditionResult)) {
            for (DLangParser.StatementContext stmt : ctx.thenBlock) {
                visit(stmt);
            }
        } else if (ctx.elseBlock != null) {
            for (DLangParser.StatementContext stmt : ctx.elseBlock) {
                visit(stmt);
            }
        }
        return null;
    }

    @Override
    public Object visitExpression(DLangParser.ExpressionContext ctx) {
        return visit(ctx.additive());
    }

    @Override
    public Object visitAdditive(DLangParser.AdditiveContext ctx) {
        Object leftObj = visit(ctx.multiplicative(0));
        Integer left = (Integer) leftObj;

        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            Object rightObj = visit(ctx.multiplicative(i));
            Integer right = (Integer) rightObj;

            if (operator.equals("+")) {
                left += right;
            } else if (operator.equals("-")) {
                left -= right;
            }
        }
        return left;
    }

    @Override
    public Object visitMultiplicative(DLangParser.MultiplicativeContext ctx) {
        Object leftObj = visit(ctx.primary(0));
        Integer left = (Integer) leftObj;

        for (int i = 1; i < ctx.primary().size(); i++) {
            String operator = ctx.getChild(2 * i - 1).getText();
            Object rightObj = visit(ctx.primary(i));
            Integer right = (Integer) rightObj;

            if (operator.equals("*")) {
                left *= right;
            } else if (operator.equals("/")) {
                left /= right;
            }
        }
        return left;
    }

    @Override
    public Object visitPrimary(DLangParser.PrimaryContext ctx) {
        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        } else if (ctx.ID() != null) {
            return variables.getOrDefault(ctx.ID().getText(), 0);
        }
        return visit(ctx.expression());
    }
}

