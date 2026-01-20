package nl.finnt730;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class LangEvaluatorVisitor2 extends DLangBaseVisitor<Object> {

    private final Stack<Map<String, Object>> scopes = new Stack<>();
    private final Map<String, Function> functions = new HashMap<>();

    public LangEvaluatorVisitor2() {
        scopes.push(new HashMap<>()); // Global scope
    }

    private static class Function {
        List<String> paramNames; // Only names, types ignored for now
        DLangParser.BlockContext block;

        Function(List<String> paramNames, DLangParser.BlockContext block) {
            this.paramNames = paramNames;
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

    // --- Visitors ---

    @Override
    public Object visitClassDeclaration(DLangParser.ClassDeclarationContext ctx) {
        // Execute all member declarations (function registrations)
        for (DLangParser.MemberDeclarationContext member : ctx.memberDeclaration()) {
            visit(member);
        }

        // After registration, look for 'main' function to execute
        if (functions.containsKey("main")) {
            Function mainFunc = functions.get("main");
            scopes.push(new HashMap<>()); // Main scope
            try {
                visit(mainFunc.block);
            } catch (ReturnValue rv) {
                // Main returned
            } finally {
                scopes.pop();
            }
        }
        return null;
    }

    @Override
    public Object visitMemberDeclaration(DLangParser.MemberDeclarationContext ctx) {
        // ID DCOLON FUNC LPAREN parameters? RPAREN block
        // Context has children. index 0 is optional static.
        // ID is the function name.
        String name = ctx.ID().getText();

        List<String> params = new ArrayList<>();
        if (ctx.parameters() != null) {
            for (DLangParser.ParameterContext p : ctx.parameters().parameter()) {
                params.add(p.ID().getText());
            }
        }

        functions.put(name, new Function(params, ctx.block()));
        return null;
    }

    @Override
    public Object visitVariableDeclaration(DLangParser.VariableDeclarationContext ctx) {
        // (type | VAR) ID ASSIGN expression
        String id = ctx.ID().getText();
        Object value = visit(ctx.expression());
        scopes.peek().put(id, value);
        return null;
    }

    @Override
    public Object visitAssignmentStatement(DLangParser.AssignmentStatementContext ctx) {
        String id = ctx.ID().getText();
        List<Object> indices = new ArrayList<>();
        // Collect indices.
        // ctx.expression() returns list of expressions.
        // If (LBRACK expression RBRACK)* is present, we have N expressions for indices.
        // And one expression at end for value.
        // Check LBRACK count.
        int bracketCount = ctx.LBRACK().size();

        List<DLangParser.ExpressionContext> exprs = ctx.expression();
        // Assuming the last expression is the value to assign.
        // And the first 'bracketCount' expressions are indices.

        Object value = visit(exprs.get(exprs.size() - 1));

        for (int i = 0; i < bracketCount; i++) {
             indices.add(visit(exprs.get(i)));
        }

        // Find variable
        Map<String, Object> scope = null;
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(id)) {
                scope = scopes.get(i);
                break;
            }
        }

        if (scope == null) throw new RuntimeException("Variable not assigned: " + id);

        if (indices.isEmpty()) {
            scope.put(id, value);
        } else {
             Object current = scope.get(id);
             // Traverse to the last container
             for (int i = 0; i < indices.size() - 1; i++) {
                  Object idx = indices.get(i);
                  if (current instanceof List) {
                      current = ((List<?>)current).get((Integer)idx);
                  } else if (current instanceof char[]) {
                       // Cannot proceed deeper into primitive array unless it was Object[] but char[] is leaf usually?
                       throw new RuntimeException("Cannot index into char array for multi-dimensional access");
                  }
             }

             // Set value at last index
             Object lastIndex = indices.get(indices.size() - 1);
             if (current instanceof List) {
                 ((List<Object>)current).set((Integer)lastIndex, value);
             } else if (current instanceof char[]) {
                 ((char[])current)[(Integer)lastIndex] = (Character)value; // Assuming value is char?
                 // Or cast depending on what logic you want.
             }
        }
        return null;
    }

    @Override
    public Object visitExpressionStatement(DLangParser.ExpressionStatementContext ctx) {
        visit(ctx.expression());
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
    public Object visitWhileStatement(DLangParser.WhileStatementContext ctx) {
        while (asBoolean(visit(ctx.condition()))) {
            visit(ctx.block());
        }
        return null;
    }

    @Override
    public Object visitForeachStatement(DLangParser.ForeachStatementContext ctx) {
        // FOREACH LPAREN type ID COLON expression RPAREN block
        String varName = ctx.ID().getText();
        Object collection = visit(ctx.expression());

        if (collection instanceof List) {
            List<?> list = (List<?>) collection;
            for (Object item : list) {
                scopes.peek().put(varName, item);
                visit(ctx.block());
            }
        } else if (collection instanceof char[]) {
             char[] arr = (char[]) collection;
             for (char c : arr) {
                 scopes.peek().put(varName, c);
                 visit(ctx.block());
             }
        } else {
            throw new RuntimeException("Foreach expects a list or array");
        }
        return null;
    }

    @Override
    public Object visitReturnStatement(DLangParser.ReturnStatementContext ctx) {
         Object val = ctx.expression() != null ? visit(ctx.expression()) : null;
        throw new ReturnValue(val);
    }

    @Override
    public Object visitPrimary(DLangParser.PrimaryContext ctx) {
        Object result = null;
        int startType = ctx.start.getType();

        if (startType == DLangParser.INT) {
            result = Integer.parseInt(ctx.INT().getText());
        } else if (startType == DLangParser.STRING) {
            String s = ctx.STRING().getText();
            result = s.substring(1, s.length() - 1);
        } else if (startType == DLangParser.CHAR) {
            String c = ctx.CHAR().getText();
            result = c.charAt(1);
        } else if (startType == DLangParser.BOOL) {
            result = Boolean.parseBoolean(ctx.BOOL().getText());
        } else if (startType == DLangParser.LPAREN) {
            // ( expression )
            // expression is first child inside parens.
            // LPAREN expression RPAREN
            result = visit(ctx.expression(0));
        } else if (startType == DLangParser.LBRACK) {
            // [ ... ]
            List<Object> list = new ArrayList<>();
            if (ctx.listContents(0) != null) {
                for (DLangParser.ExpressionContext exp : ctx.listContents(0).expression()) {
                    list.add(visit(exp));
                }
            }
            result = list;
        } else if (startType == DLangParser.STATIC) {
            // STATIC matches start. Next is DCOLON -> ID -> LPAREN
            String method = ctx.ID(0).getText(); // Only one ID in this alt
            List<Object> args = getArgs(ctx.listContents(0));

            if (functions.containsKey(method)) {
                 result = callFunction(method, args);
            } else {
                 throw new RuntimeException("Static function " + method + " not found.");
            }
        } else if (startType == DLangParser.ID) {
            // Check second token to distinguish Call vs Var vs Static Call
            int secondType = -1;
            if (ctx.getChildCount() > 1 && ctx.getChild(1) instanceof TerminalNode) {
                 secondType = ((TerminalNode)ctx.getChild(1)).getSymbol().getType();
            }

            if (secondType == DLangParser.LPAREN) {
                 // ID ( ... ) -> Function Call
                 String func = ctx.ID(0).getText();
                 List<Object> args = getArgs(ctx.listContents(0));
                 result = callFunction(func, args);
            } else if (secondType == DLangParser.DCOLON) {
                 // ID :: ID ( ... )
                 String cls = ctx.ID(0).getText();
                 String method = ctx.ID(1).getText();
                 List<Object> args = getArgs(ctx.listContents(0));

                 if (cls.equals("system") && method.equals("println")) {
                      String output = "";
                      for(Object arg : args) output += arg;
                      System.out.println(output);
                      result = null;
                 } else {
                      if (functions.containsKey(method)) {
                          result = callFunction(method, args);
                      } else {
                          throw new RuntimeException("Static function " + method + " not found.");
                      }
                 }
            } else {
                 // ID (Variable)
                 result = resolveVariable(ctx.ID(0).getText());
            }
        }

        // Loop for suffixes: [expr] or .id(...)
        // We iterate children.
        // We need to keep track of expression index and listContents index.
        // Since we visited some expressions/listContents in the 'start' block, start counters appropriately.

        // Count how many expressions/listContents were consumed by start block
        int exprIdx = 0;
        int listIdx = 0;

        if (startType == DLangParser.LPAREN) exprIdx = 1; // ( expr )
        else if (startType == DLangParser.LBRACK) listIdx = 1; // [ list? ]
        else if (startType == DLangParser.ID) {
             int secondType = -1;
             if (ctx.getChildCount() > 1 && ctx.getChild(1) instanceof TerminalNode)
                  secondType = ((TerminalNode)ctx.getChild(1)).getSymbol().getType();

             if (secondType == DLangParser.LPAREN) listIdx = 1;
             else if (secondType == DLangParser.DCOLON) listIdx = 1;
             else if (startType == DLangParser.STATIC) listIdx = 1;
        }

        for (int i = 0; i < ctx.getChildCount(); i++) {
             ParseTree child = ctx.getChild(i);
             if (child instanceof TerminalNode) {
                 int type = ((TerminalNode)child).getSymbol().getType();
                 if (type == DLangParser.DOT) {
                     // . id ( ... )
                     // i is DOT
                     String methodName = ctx.getChild(i+1).getText();

                     // Check if listContents exists.
                     // DOT ID LPAREN listContents? RPAREN
                     // i+2 is LPAREN. i+3 is listContents or RPAREN.
                     ParseTree possibleList = ctx.getChild(i+3);
                     List<Object> args = new ArrayList<>();
                     if (possibleList instanceof DLangParser.ListContentsContext) {
                          args = getArgs((DLangParser.ListContentsContext)possibleList);
                          // We consumed one listContents
                          listIdx++;
                     }

                     if (result instanceof String && methodName.equals("toCharArray")) {
                          result = ((String)result).toCharArray();
                     } else if (result instanceof List && methodName.equals("add")) {
                          ((List<Object>)result).add(args.get(0));
                     } else if (result instanceof List && methodName.equals("size")) {
                           result = ((List<?>)result).size();
                     }

                 } else if (type == DLangParser.LBRACK) {
                     // Check if it's start type [ ... ] or suffix [ ... ]
                     // If it's the very first token, it's array creation, already handled.
                     if (i == 0) continue;

                     // suffix [ expr ]
                     // Handled here.
                     // i is LBRACK. i+1 is expression. i+2 is RBRACK.
                     // But wait, the child at i+1 IS the ExpressionContext.
                     // The generic ctx.expression(exprIdx) gives us the contexts in order.
                     ParseTree exprChild = ctx.getChild(i+1);
                     if (exprChild instanceof DLangParser.ExpressionContext) {
                         // This is safer than relying on exprIdx if we scan linear.
                         // But we can use visits.
                         Object index = visit(exprChild);
                         if (result instanceof List) {
                              result = ((List<?>)result).get((Integer)index);
                         } else if (result instanceof char[]) {
                              result = ((char[])result)[(Integer)index];
                         }
                     }
                 }
             }
        }

        return result;
    }

    // Helper to extract args
    private List<Object> getArgs(DLangParser.ListContentsContext ctx) {
        List<Object> list = new ArrayList<>();
        if (ctx != null) {
            for (DLangParser.ExpressionContext exp : ctx.expression()) {
                list.add(visit(exp));
            }
        }
        return list;
    }

    private Object callFunction(String name, List<Object> args) {
         if (functions.containsKey(name)) {
             Function f = functions.get(name);
             if (f.paramNames.size() != args.size()) {
                 // In basic.dl main takes args, but we might call it without?
                 // Allow mismatch for main?
             }

             Map<String, Object> newScope = new HashMap<>();
             for (int i = 0; i < f.paramNames.size() && i < args.size(); i++) {
                 newScope.put(f.paramNames.get(i), args.get(i));
             }
             scopes.push(newScope);
             try {
                 visit(f.block);
             } catch (ReturnValue rv) {
                 return rv.value;
             } finally {
                 scopes.pop();
             }
             return null;
         }
         throw new RuntimeException("Function " + name + " not found.");
    }

    // --- Expression Logic ---
    @Override
    public Object visitCondition(DLangParser.ConditionContext ctx) {
        Object left = visit(ctx.expression(0));
        if (ctx.operator == null) return left;
        Object right = visit(ctx.expression(1));

        int type = ctx.operator.getType();
        if (type == DLangParser.EQ) return Objects.equals(left, right);
        if (type == DLangParser.NEQ) return !Objects.equals(left, right);
        if (type == DLangParser.GRT && left instanceof Integer && right instanceof Integer) return (Integer)left > (Integer)right;
        if (type == DLangParser.LST && left instanceof Integer && right instanceof Integer) return (Integer)left < (Integer)right;
        return false;
    }

    @Override
    public Object visitAdditive(DLangParser.AdditiveContext ctx) {
        Object left = visit(ctx.multiplicative(0));
        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            Object right = visit(ctx.multiplicative(i));
            if (op.equals("+")) {
                if (left instanceof String || right instanceof String) return left.toString() + right.toString();
                if (left instanceof Integer && right instanceof Integer) left = (Integer)left + (Integer)right;
            } else if (op.equals("-")) {
                 if (left instanceof Integer && right instanceof Integer) left = (Integer)left - (Integer)right;
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
                if (op.equals("*")) left = (Integer)left * (Integer)right;
                if (op.equals("/")) left = (Integer)left / (Integer)right;
            }
        }
        return left;
    }

    @Override
    public Object visitExpression(DLangParser.ExpressionContext ctx) {
        return visit(ctx.additive());
    }

    private boolean asBoolean(Object obj) {
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Integer) return (Integer) obj != 0;
        return obj != null;
    }

    @Override
    public Object visitDebug(DLangParser.DebugContext ctx) {
        System.out.println("DEBUG: " + visit(ctx.primary()));
        return super.visitDebug(ctx);
    }
}
