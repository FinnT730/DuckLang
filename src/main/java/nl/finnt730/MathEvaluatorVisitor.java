package nl.finnt730;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class MathEvaluatorVisitor extends DLangBaseVisitor<Object> {

    private final Stack<Map<String, Object>> scopes = new Stack<>();
    private final Map<String, Function> functions = new HashMap<>();

    public MathEvaluatorVisitor() {
        scopes.push(new HashMap<>()); // Global scope
    }

    private static class Function {
        List<String> params;
        DLangParser.BlockContext block;

        Function(List<String> params, DLangParser.BlockContext block) {
            this.params = params;
            this.block = block;
        }
    }

    private static class ReturnValue extends RuntimeException {
        public final Object value;
        public ReturnValue(Object value) { this.value = value; }
    }

    private Object resolveVariable(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        throw new RuntimeException("Undefined variable: " + name);
    }

    private boolean asBoolean(Object obj) {
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Integer) return (Integer) obj != 0;
        return obj != null;
    }

    @Override
    public Object visitFunctionDeclaration(DLangParser.FunctionDeclarationContext ctx) {
        String name = ctx.ID().getText();
        List<String> params = new ArrayList<>();
        if (ctx.parameters() != null) {
            for (TerminalNode node : ctx.parameters().ID()) {
                params.add(node.getText());
            }
        }
        functions.put(name, new Function(params, ctx.block()));
        return null;
    }

    @Override
    public Object visitFunctionCall(DLangParser.FunctionCallContext ctx) {
        String funcName = ctx.ID().getText();

        if(Objects.equals(funcName, "recursiveFib")) {
            System.out.println("Calling recursiveFib");
        }

        Function func = functions.get(funcName);
        if (func == null) {
            throw new RuntimeException("Undefined function: " + funcName);
        }

        List<Object> args = new ArrayList<>();
        if (ctx.listContents() != null) {
            for (DLangParser.ExpressionContext expr : ctx.listContents().expression()) {
                args.add(visit(expr));
            }
        }

        if (args.size() != func.params.size()) {
            throw new RuntimeException("Function " + funcName + " expects " + func.params.size() + " arguments.");
        }

        Map<String, Object> newScope = new HashMap<>();
        for (int i = 0; i < args.size(); i++) {
            newScope.put(func.params.get(i), args.get(i));
        }

        scopes.push(newScope);
        try {
            visit(func.block);
        } catch (ReturnValue rv) {
            return rv.value;
        } finally {
            scopes.pop();
        }
        return null;
    }

    @Override
    public Object visitReturnStatement(DLangParser.ReturnStatementContext ctx) {
        Object val = ctx.expression() != null ? visit(ctx.expression()) : null;
        throw new ReturnValue(val);
    }

    @Override
    public Object visitVariableStatement(DLangParser.VariableStatementContext ctx) {
        String id = ctx.ID().getText();
        Object value = visit(ctx.expression());
        scopes.peek().put(id, value);
        return null;
    }

    @Override
    public Object visitAssignmentStatement(DLangParser.AssignmentStatementContext ctx) {
        String id = ctx.ID().getText();
        Object value = visit(ctx.expression());

        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(id)) {
                scopes.get(i).put(id, value);
                return null;
            }
        }
        throw new RuntimeException("Variable not defined: " + id);
    }

    @Override
    public Object visitWhileStatement(DLangParser.WhileStatementContext ctx) {
        while (asBoolean(visit(ctx.condition()))) {
            visit(ctx.block());
        }
        return null;
    }

    @Override
    public Object visitIfStatement(DLangParser.IfStatementContext ctx) {
        if (asBoolean(visit(ctx.condition()))) {
            visit(ctx.block(0));
        } else if (ctx.block().size() > 1) {
            visit(ctx.block(1));
        }
        return null;
    }

    @Override
    public Object visitPrintStatement(DLangParser.PrintStatementContext ctx) {
        Object val = visit(ctx.expression());
        System.out.println(val);
        return null;
    }

    @Override
    public Object visitExpressionStatement(DLangParser.ExpressionStatementContext ctx) {
        visit(ctx.expression());
        return null;
    }

    @Override
    public Object visitIntegerLiteral(DLangParser.IntegerLiteralContext ctx) {
        return Integer.parseInt(ctx.getText());
    }

    @Override
    public Object visitStringLiteral(DLangParser.StringLiteralContext ctx) {
        String text = ctx.getText();
        return text.substring(1, text.length() - 1);
    }

    @Override
    public Object visitBooleanLiteral(DLangParser.BooleanLiteralContext ctx) {
        return Boolean.parseBoolean(ctx.getText());
    }

    @Override
    public Object visitArrayLiteral(DLangParser.ArrayLiteralContext ctx) {
        List<Object> list = new ArrayList<>();
        if (ctx.listContents() != null) {
            for (DLangParser.ExpressionContext expr : ctx.listContents().expression()) {
                list.add(visit(expr));
            }
        }
        return list;
    }

    @Override
    public Object visitArrayAccess(DLangParser.ArrayAccessContext ctx) {
        String id = ctx.ID().getText();
        Object arrObj = resolveVariable(id);
        Object indexObj = visit(ctx.expression());

        if (arrObj instanceof List && indexObj instanceof Integer) {
            return ((List<?>) arrObj).get((Integer) indexObj);
        }
        throw new RuntimeException("Invalid array access");
    }

    @Override
    public Object visitIdentifier(DLangParser.IdentifierContext ctx) {
        return resolveVariable(ctx.getText());
    }

    @Override
    public Object visitParenthesizedExpression(DLangParser.ParenthesizedExpressionContext ctx) {
         return visit(ctx.expression());
    }

    @Override
    public Object visitCondition(DLangParser.ConditionContext ctx) {
        Object left = visit(ctx.expression(0));
        if (ctx.operator == null) return left;

        Object right = visit(ctx.expression(1));

        int type = ctx.operator.getType();

        if (type == DLangParser.EQ) return Objects.equals(left, right);
        if (type == DLangParser.NEQ) return !Objects.equals(left, right);

        if (left instanceof Integer && right instanceof Integer) {
            int l = (Integer) left;
            int r = (Integer) right;
            if (type == DLangParser.GRT) return l > r;
            if (type == DLangParser.LST) return l < r;
        }

        return false;
    }

    @Override
    public Object visitAdditive(DLangParser.AdditiveContext ctx) {
        Object left = visit(ctx.multiplicative(0));

        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Object right = visit(ctx.multiplicative(i));

            if (op.equals("+")) {
                if (left instanceof String || right instanceof String) {
                    left = left.toString() + right.toString();
                } else if (left instanceof Integer && right instanceof Integer) {
                    left = (Integer) left + (Integer) right;
                }
            } else if (op.equals("-")) {
                 if (left instanceof Integer && right instanceof Integer) {
                    left = (Integer) left - (Integer) right;
                }
            }
        }
        return left;
    }

    @Override
    public Object visitMultiplicative(DLangParser.MultiplicativeContext ctx) {
        Object left = visit(ctx.primary(0));

        for (int i = 1; i < ctx.primary().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Object right = visit(ctx.primary(i));

            if (left instanceof Integer && right instanceof Integer) {
                if (op.equals("*")) left = (Integer) left * (Integer) right;
                else if (op.equals("/")) left = (Integer) left / (Integer) right;
            }
        }
        return left;
    }

    @Override
    public Object visitExpression(DLangParser.ExpressionContext ctx) {
        return visit(ctx.additive());
    }
}

