package com.lang.evaluator;

import java.util.ArrayDeque;
import java.util.Deque;

import com.lang.ast.*;
import com.lang.execution.Execution;
import com.lang.util.StringUtils;
import com.lang.value.BlockValue;
import com.lang.value.CallableValue;
import com.lang.value.Value;
import com.lang.value.ValueResult;

public class Evaluator implements Execution {
    private final Deque<BlockValue> scopes;

    private final String workingDir;

    public Evaluator(String workingDir) {
        this.scopes = new ArrayDeque<>();
        this.workingDir = workingDir;
    }

    @Override
    public String workingDir() {
        return workingDir;
    }

    @Override
    public ValueResult execute(Expr expr) {
        return (ValueResult) expr.accept(this);
    }

    @Override
    public BlockValue peekScope() {
        return scopes.peek();
    }

    @Override
    public void pushScope(BlockValue scope) {
        scopes.push(scope);
    }

    @Override
    public BlockValue popScope() {
        return scopes.pop();
    }

    @Override
    public ValueResult execute(Program program, BlockValue initScope) {
        if (initScope != null) {
            pushScope(initScope);
        } else {
            pushScope(new BlockValue());
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

        if (initScope != null) {
            popScope();
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
                result = Value.ofBool(Boolean.parseBoolean(expr.lexeme));
                break;
            case LiteralExpr.INT:
                result = Value.ofInt(Long.parseLong(expr.lexeme));
                break;
            case LiteralExpr.FLOAT:
                result = Value.ofFloat(Double.parseDouble(expr.lexeme));
                break;
            case LiteralExpr.STRING:
                String str = StringUtils.stripQuotes(expr.lexeme);
                str = StringUtils.unescape(str);
                str = StringUtils.replace(str, peekScope().getLocals());
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
        BlockValue scope = peekScope();

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
            peekScope().set(name, value);
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
            long l = left.asInt().getValue();
            long r = right.asInt().getValue();
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
            double l = left.isInt() ? left.asInt().getValue() : left.asFloat().getValue();
            double r = right.isInt() ? right.asInt().getValue() : right.asFloat().getValue();
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
            boolean l = left.asBool().getValue();
            boolean r = right.asBool().getValue();
            switch (op) {
                case "&&":
                    result = Value.ofBool(l && r);
                    break;
                case "||":
                    result = Value.ofBool(l || r);
                    break;
            }
        } else if (left.isString() && right.isString()) {
            String l = left.valueToString();
            String r = right.valueToString();
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
                result = Value.ofString(left.valueToString() + right.toString());
            } else if (right.isString()) {
                result = Value.ofString(left.toString() + right.valueToString());
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
                result = Value.ofBool(!operand.asBool().getValue());
                break;
            case "-":
                if (operand.isInt()) {
                    result = Value.ofInt(-operand.asInt().getValue());
                } else if (operand.isFloat()) {
                    result = Value.ofFloat(-operand.asFloat().getValue());
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

        if (!callee.isCallable()) {
            throw new RuntimeException("Cannot call: " + callee.getClass().getSimpleName());
        }

        CallableValue calleeValue = callee.asCallable();

        return calleeValue.call(this, expr.arguments, expr.bindings);
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
        final BlockValue closureScope = peekScope();
        BlockValue blockValue = new BlockValue(expr, closureScope);

        return ValueResult.of(ValueResult.NORMAL, null, blockValue);
    }

    @Override
    public Object visitParenthesizedExpr(ParenthesizedExpr expr) {
        return expr.inner.accept(this);
    }
}