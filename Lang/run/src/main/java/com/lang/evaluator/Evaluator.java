package com.lang.evaluator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;

import com.lang.ast.*;
import com.lang.context.Context;
import com.lang.value.BlockValue;
import com.lang.value.Value;
import com.lang.value.ValueResult;

public class Evaluator implements Context, ExprVisitor {
    private final Deque<BlockValue> stack;
    private static final BlockValue GLOBAL = new BlockValue();

    static {
        initGlobal();
    }

    public Evaluator() {
        this.stack = new ArrayDeque<>();
        this.stack.push(GLOBAL);
    }

    @Override
    public String getName() {
        return "Evaluator";
    }

    @Override
    public ExprVisitor visitor() {
        return this;
    }

    @Override
    public Deque<BlockValue> stack() {
        return stack;
    }

    public BlockValue peek() {
        return stack.peek();
    }

    public BlockValue push(BlockValue scope) {
        stack.push(scope);
        return scope;
    }

    public BlockValue pop() {
        return stack.pop();
    }

    private static void initGlobal() {
        GLOBAL.set(
                "if",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (arguments.size() != 1) {
                            throw new RuntimeException("if(condition) expects 1 argument");
                        }

                        if (bindings.size() != 1 && bindings.size() != 2) {
                            throw new RuntimeException(
                                    "if(condition) requires only 1 or 2 bindings (then) or (then, else)");
                        }

                        ValueResult condResult = (ValueResult) arguments.get(0).accept(ctx.visitor());

                        if (condResult.isLaunched()) {
                            return condResult;
                        }

                        Value cond = condResult.getValue();

                        if (!cond.isBool()) {
                            throw new RuntimeException("if() condition must be boolean");
                        }

                        Expr chosen = null;
                        if (bindings.size() == 1) {
                            chosen = cond.toLBool() ? bindings.get(0) : null;
                        } else if (bindings.size() == 2) {
                            chosen = cond.toLBool() ? bindings.get(0) : bindings.get(1);
                        }

                        if (chosen == null) {
                            return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                        }

                        ValueResult blockResult = (ValueResult) chosen.accept(ctx.visitor());

                        if (blockResult.isLaunched()) {
                            return blockResult;
                        }

                        Value blockValue = blockResult.getValue();

                        if (!blockValue.isBlock()) {
                            throw new RuntimeException("if() then and else must be blocks");
                        }

                        return blockValue
                                .asBlock()
                                .call(ctx, new ArrayList<>(), new ArrayList<>());
                    }

                });

        GLOBAL.set(
                "while",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (arguments.size() != 1) {
                            throw new RuntimeException("while(condition) expects 1 argument");
                        }

                        if (bindings.size() != 1) {
                            throw new RuntimeException(
                                    "while(condition) requires 1 attachment (body)");
                        }

                        Expr condition = arguments.get(0);
                        Expr body = bindings.get(0);

                        ValueResult result = ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());

                        while (true) {
                            ValueResult condResult = (ValueResult) condition.accept(ctx.visitor());

                            if (condResult.isLaunched()) {
                                if (condResult.getId().equals("break")) {
                                    result = ValueResult.of(
                                            ValueResult.NORMAL,
                                            null,
                                            condResult.getValue());
                                    break;
                                } else if (condResult.getId().equals("continue")) {
                                    continue;
                                } else {
                                    return condResult;
                                }
                            }

                            Value cond = condResult.getValue();

                            if (!cond.isBool()) {
                                throw new RuntimeException("while() condition must return boolean");
                            }

                            if (!cond.toLBool()) {
                                break;
                            }

                            ValueResult bodyResult = (ValueResult) body.accept(ctx.visitor());

                            if (bodyResult.isLaunched()) {
                                if (bodyResult.getId().equals("break")) {
                                    result = ValueResult.of(
                                            ValueResult.NORMAL,
                                            null,
                                            bodyResult.getValue());
                                    break;
                                } else if (bodyResult.getId().equals("continue")) {
                                    continue;
                                } else {
                                    return bodyResult;
                                }
                            }

                            Value bodyValue = bodyResult.getValue();

                            if (bodyValue != null && bodyValue.isBlock()) {
                                result = bodyValue
                                        .asBlock()
                                        .call(
                                                ctx,

                                                new ArrayList<>(),
                                                new ArrayList<>());
                            } else {
                                result = ValueResult.of(ValueResult.NORMAL, null, bodyValue);
                            }
                        }

                        return result;
                    }

                });

        GLOBAL.set(
                "break",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (arguments.size() > 1) {
                            throw new RuntimeException("break() expects 0 or 1 arguments");
                        }
                        if (arguments.size() == 1) {
                            ValueResult v = (ValueResult) arguments.get(0).accept(ctx.visitor());
                            return ValueResult.of(ValueResult.LAUNCHED, "break", v.getValue());
                        }
                        return ValueResult.of(ValueResult.LAUNCHED, "break", Value.ofNull());
                    }

                });

        GLOBAL.set(
                "continue",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (!arguments.isEmpty()) {
                            throw new RuntimeException("continue() expects 0 arguments");
                        }
                        return ValueResult.of(ValueResult.LAUNCHED, "continue", Value.ofNull());
                    }

                });

        GLOBAL.set(
                "println",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (arguments.size() == 0) {
                            System.out.println();
                        } else {
                            for (Expr arg : arguments) {
                                ValueResult v = (ValueResult) arg.accept(ctx.visitor());
                                if (v.isLaunched()) {
                                    return v;
                                }
                                System.out.println(v.getValue().toLString());
                            }
                        }
                        return ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
                    }

                });

        GLOBAL.set(
                "readln",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {

                        if (!arguments.isEmpty()) {
                            throw new RuntimeException("readln() expects 0 arguments");
                        }

                        Scanner scanner = new Scanner(System.in);
                        String line = scanner.nextLine();

                        return ValueResult.of(
                                ValueResult.NORMAL, null, Value.ofString(line));
                    }

                });

        GLOBAL.set(
                "capture",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (arguments.size() != 1) {
                            throw new RuntimeException("capture(expr) expects 1 argument");
                        }

                        ValueResult result = (ValueResult) arguments.get(0).accept(ctx.visitor());

                        if (result.isLaunched() && result.getId().equals("return")) {
                            return ValueResult.of(ValueResult.NORMAL, null, result.getValue());
                        }

                        return result;
                    }

                });

        GLOBAL.set(
                "return",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {
                        if (arguments.size() != 1) {
                            throw new RuntimeException("return() expects 1 argument");
                        }
                        ValueResult v = (ValueResult) arguments.get(0).accept(ctx.visitor());
                        return ValueResult.of(ValueResult.LAUNCHED, "return", v.getValue());
                    }

                });

        GLOBAL.set(
                "launch",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {

                        if (arguments.size() != 2) {
                            throw new RuntimeException("launch(id, value) expects 2 arguments");
                        }

                        ValueResult idResult = (ValueResult) arguments.get(0).accept(ctx.visitor());

                        if (idResult.isLaunched()) {
                            return idResult;
                        }

                        Value id = idResult.getValue();

                        if (!id.isString()) {
                            throw new RuntimeException("launch() id must be string");
                        }

                        ValueResult valueResult = (ValueResult) arguments.get(1).accept(ctx.visitor());

                        if (valueResult.isLaunched()) {
                            return valueResult;
                        }

                        return ValueResult.of(
                                ValueResult.LAUNCHED, id.toLString(), valueResult.getValue());
                    }

                });

        GLOBAL.set(
                "finalize",
                new BlockValue() {
                    @Override
                    public ValueResult call(Context ctx, List<Expr> arguments, List<Expr> bindings) {

                        if (arguments.size() != 2) {
                            throw new RuntimeException("finalize(id, body) expects 2 arguments");
                        }

                        ValueResult idResult = (ValueResult) arguments.get(0).accept(ctx.visitor());

                        if (idResult.isLaunched()) {
                            return idResult;
                        }

                        Value id = idResult.getValue();

                        if (!id.isString()) {
                            throw new RuntimeException("finalize() id must be string");
                        }

                        ValueResult bodyResult = (ValueResult) arguments.get(1).accept(ctx.visitor());

                        if (bodyResult.isLaunched() && bodyResult.getId().equals(id.toLString())) {
                            return ValueResult.of(ValueResult.NORMAL, null, bodyResult.getValue());
                        }

                        return bodyResult;
                    }

                });
    }

    public BlockValue getGlobal() {
        return GLOBAL;
    }

    public ValueResult evaluate(Program program) {
        return evaluate(program, null);
    }

    public ValueResult evaluate(Program program, BlockValue entryBlock) {
        if (entryBlock != null) {
            push(entryBlock);
        } else {
            push(new BlockValue(null, GLOBAL));
        }

        ValueResult result = ValueResult.of(ValueResult.NORMAL, null, Value.ofNull());
        for (Expr expr : program.expressions) {
            result = (ValueResult) expr.accept(this);
            if (result.isLaunched()) {
                if (result.getId().equals("return")) {
                    result = ValueResult.of(ValueResult.NORMAL, null, result.getValue());
                }
                break;
            }
        }

        if (entryBlock != null) {
            pop();
        }
        return result;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr) {
        Value result;
        switch (expr.type) {
            case LiteralExpr.NULL:
                result = Value.ofNull();
                break;
            case LiteralExpr.BOOL:
                result = Value.ofBool(Boolean.parseBoolean(expr.value));
                break;
            case LiteralExpr.INT:
                result = Value.ofInt(Long.parseLong(expr.value));
                break;
            case LiteralExpr.FLOAT:
                result = Value.ofFloat(Double.parseDouble(expr.value));
                break;
            case LiteralExpr.STRING:
                String str = expr.value;
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = str.substring(1, str.length() - 1);
                }
                result = Value.ofString(str);
                break;
            default:
                result = Value.ofNull();
        }
        return ValueResult.of(ValueResult.NORMAL, null, result);
    }

    @Override
    public Object visitRefExpr(RefExpr expr) {
        String name = expr.name.source;
        BlockValue scope = peek();

        if (scope.has(name)) {
            return ValueResult.of(ValueResult.NORMAL, null, scope.get(name));
        }

        throw new RuntimeException("Undefined variable: " + name);
    }

    @Override
    public Object visitAssignExpr(AssignExpr expr) {
        ValueResult valueResult = (ValueResult) expr.value.accept(this);

        if (valueResult.isLaunched()) {
            return valueResult;
        }

        Value value = valueResult.getValue();

        if (expr.target instanceof RefExpr) {
            String name = ((RefExpr) expr.target).name.source;
            peek().set(name, value);
            return ValueResult.of(ValueResult.NORMAL, null, value);
        }

        if (expr.target instanceof MemberAccessExpr) {
            MemberAccessExpr member = (MemberAccessExpr) expr.target;
            ValueResult objectResult = (ValueResult) member.object.accept(this);

            if (objectResult.isLaunched()) {
                return objectResult;
            }

            Value object = objectResult.getValue();

            if (object.isBlock()) {
                object.asBlock().setLocal(member.name.source, value);
                return ValueResult.of(ValueResult.NORMAL, null, value);
            }
            throw new RuntimeException(
                    "Cannot assign member of " + object.getClass().getSimpleName());
        }

        throw new RuntimeException(
                "Invalid assignment target: " + expr.target.getClass().getSimpleName());
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr) {
        ValueResult leftResult = (ValueResult) expr.left.accept(this);
        if (leftResult.isLaunched()) {
            return leftResult;
        }

        ValueResult rightResult = (ValueResult) expr.right.accept(this);
        if (rightResult.isLaunched()) {
            return rightResult;
        }

        Value left = leftResult.getValue();
        Value right = rightResult.getValue();

        String op = expr.operator.lexeme;
        Value result = null;

        if (op.equals("==")) {
            result = Value.ofBool((left.isNull() && right.isNull()) || left.equals(right));
        } else if (op.equals("!=")) {
            result = Value.ofBool(!((left.isNull() && right.isNull()) || left.equals(right)));
        } else if (left.isInt() && right.isInt()) {
            long l = left.toLInt();
            long r = right.toLInt();
            switch (op) {
                case "+":
                    result = Value.ofInt(l + r);
                    break;
                case "-":
                    result = Value.ofInt(l - r);
                    break;
                case "*":
                    result = Value.ofInt(l * r);
                    break;
                case "/":
                    result = Value.ofInt(l / r);
                    break;
                case "%":
                    result = Value.ofInt(l % r);
                    break;
                case "<":
                    result = Value.ofBool(l < r);
                    break;
                case ">":
                    result = Value.ofBool(l > r);
                    break;
                case "<=":
                    result = Value.ofBool(l <= r);
                    break;
                case ">=":
                    result = Value.ofBool(l >= r);
                    break;
            }
        } else if ((left.isInt() || left.isFloat()) && (right.isInt() || right.isFloat())) {
            double l = left.isInt() ? left.toLInt() : left.toLFloat();
            double r = right.isInt() ? right.toLInt() : right.toLFloat();
            switch (op) {
                case "+":
                    result = Value.ofFloat(l + r);
                    break;
                case "-":
                    result = Value.ofFloat(l - r);
                    break;
                case "*":
                    result = Value.ofFloat(l * r);
                    break;
                case "/":
                    result = Value.ofFloat(l / r);
                    break;
                case "%":
                    result = Value.ofFloat(l % r);
                    break;
                case "<":
                    result = Value.ofBool(l < r);
                    break;
                case ">":
                    result = Value.ofBool(l > r);
                    break;
                case "<=":
                    result = Value.ofBool(l <= r);
                    break;
                case ">=":
                    result = Value.ofBool(l >= r);
                    break;
            }
        } else if (left.isBool() && right.isBool()) {
            boolean l = left.toLBool();
            boolean r = right.toLBool();
            switch (op) {
                case "&&":
                    result = Value.ofBool(l && r);
                    break;
                case "||":
                    result = Value.ofBool(l || r);
                    break;
            }
        } else if (left.isString() && right.isString()) {
            String l = left.toLString();
            String r = right.toLString();
            switch (op) {
                case "+":
                    result = Value.ofString(l + r);
                    break;
                case "<":
                    result = Value.ofBool(l.compareTo(r) < 0);
                    break;
                case ">":
                    result = Value.ofBool(l.compareTo(r) > 0);
                    break;
                case "<=":
                    result = Value.ofBool(l.compareTo(r) <= 0);
                    break;
                case ">=":
                    result = Value.ofBool(l.compareTo(r) >= 0);
                    break;
            }
        } else if (op.equals("+")) {
            if (left.isString()) {
                result = Value.ofString(left.toLString() + right.toString());
            } else if (right.isString()) {
                result = Value.ofString(left.toString() + right.toLString());
            }
        }

        if (result == null) {
            throw new RuntimeException(
                    "Invalid operation: "
                            + op
                            + " between "
                            + left.getClass().getSimpleName()
                            + " and "
                            + right.getClass().getSimpleName());
        }

        return ValueResult.of(ValueResult.NORMAL, null, result);
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr) {
        ValueResult operandResult = (ValueResult) expr.operand.accept(this);

        if (operandResult.isLaunched()) {
            return operandResult;
        }

        Value operand = operandResult.getValue();
        String op = expr.operator.lexeme;
        Value result = null;

        switch (op) {
            case "!":
                if (!operand.isBool()) {
                    throw new RuntimeException("! requires boolean");
                }
                result = Value.ofBool(!operand.toLBool());
                break;
            case "-":
                if (operand.isInt()) {
                    result = Value.ofInt(-operand.toLInt());
                } else if (operand.isFloat()) {
                    result = Value.ofFloat(-operand.toLFloat());
                } else {
                    throw new RuntimeException("- requires number");
                }
                break;
            case "+":
                if (operand.isInt() || operand.isFloat()) {
                    result = operand;
                } else {
                    throw new RuntimeException("+ requires number");
                }
                break;
            default:
                throw new RuntimeException("Unknown unary: " + op);
        }

        return ValueResult.of(ValueResult.NORMAL, null, result);
    }

    @Override
    public Object visitCallExpr(CallExpr expr) {
        ValueResult calleeResult = (ValueResult) expr.callee.accept(this);

        if (calleeResult.isLaunched()) {
            return calleeResult;
        }

        Value callee = calleeResult.getValue();

        if (!callee.isBlock()) {
            throw new RuntimeException("Cannot call: " + callee.getClass().getSimpleName());
        }

        BlockValue block = callee.asBlock();

        return block.call(this, expr.arguments, expr.bindings);
    }

    @Override
    public Object visitMemberAccessExpr(MemberAccessExpr expr) {
        ValueResult objectResult = (ValueResult) expr.object.accept(this);

        if (objectResult.isLaunched()) {
            return objectResult;
        }

        Value object = objectResult.getValue();
        String name = expr.name.source;

        if (object == null || object.isNull()) {
            throw new RuntimeException("Cannot access member of null");
        }

        if (object.isBlock()) {
            BlockValue block = object.asBlock();
            Value result = block.getLocal(name);
            if (result == null && !block.hasLocal(name)) {
                throw new RuntimeException("Member not found: " + name);
            }
            return ValueResult.of(ValueResult.NORMAL, null, result);
        }

        throw new RuntimeException(
                "Cannot access member: " + name + " of " + object.getClass().getSimpleName());
    }

    @Override
    public Object visitBlockExpr(BlockExpr expr) {
        final BlockValue closureScope = peek();
        BlockValue blockValue = new BlockValue(expr, closureScope);

        return ValueResult.of(ValueResult.NORMAL, null, blockValue);
    }
}
