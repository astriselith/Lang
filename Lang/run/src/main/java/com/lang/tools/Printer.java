package com.lang.tools;

import com.lang.ast.*;

public class Printer implements ExprVisitor {
    private int indent = 0;

    public String print(Program program) {
        StringBuilder sb = new StringBuilder();
        for (Expr expr : program.expressions) {
            sb.append("\n").append((String) expr.accept(this)).append(";");
        }
        return sb.toString();
    }

    private String indent() {
        if (indent == 0) return "";
        int dashes = 3 + (indent - 1) * 4;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dashes; i++) {
            sb.append("-");
        }
        return sb.toString() + " ";
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr) {
        if (expr.value == null) return "null";
        return expr.value;
    }

    @Override
    public Object visitRefExpr(RefExpr expr) {
        return expr.name.source;
    }

    @Override
    public Object visitAssignExpr(AssignExpr expr) {
        return (String) expr.target.accept(this) + " = " + (String) expr.value.accept(this);
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr) {
        return (String) expr.left.accept(this) + " " + expr.operator + " " + (String) expr.right.accept(this);
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr) {
        return expr.operator + (String) expr.operand.accept(this);
    }

    @Override
    public Object visitCallExpr(CallExpr expr) {
        StringBuilder sb = new StringBuilder();
        sb.append((String) expr.callee.accept(this));

        if (expr.hasParens) {
            sb.append("(");
            for (int i = 0; i < expr.arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append((String) expr.arguments.get(i).accept(this));
            }
            sb.append(")");
        }

        if (expr.hasColon) {
            sb.append(": ");
            for (int i = 0; i < expr.bindings.size(); i++) {
                if (i > 0) sb.append(" :: ");
                sb.append((String) expr.bindings.get(i).accept(this));
            }
        }

        return sb.toString();
    }

    @Override
    public Object visitMemberAccessExpr(MemberAccessExpr expr) {
        return (String) expr.object.accept(this) + "." + expr.name.source;
    }

    @Override
    public Object visitBlockExpr(BlockExpr expr) {
        StringBuilder sb = new StringBuilder();

        if (expr.hasParens) {
            sb.append("(");
            for (int i = 0; i < expr.parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                Param param = expr.parameters.get(i);
                sb.append(param.name.source);
            }
            sb.append(")");
        }

        if (expr.hasColon) {
            sb.append(": ");
            for (int i = 0; i < expr.attachments.size(); i++) {
                if (i > 0) sb.append(" :: ");
                Attach attach = expr.attachments.get(i);
                sb.append(attach.name.source);
            }
        }

        if (expr.hasArrow) {
            sb.append(" -> ");
        }

        if (expr.isSingleExpr) {
            if (expr.expressions != null && !expr.expressions.isEmpty()) {
                sb.append((String) expr.expressions.get(0).accept(this));
            }
        } else {
            sb.append("{");
            if (expr.expressions != null && !expr.expressions.isEmpty()) {
                indent++;
                for (Expr expression : expr.expressions) {
                    sb.append("\n").append(indent()).append((String) expression.accept(this)).append(";");
                }
                indent--;
                sb.append("\n").append(indent());
            }
            sb.append("}");
        }

        return sb.toString();
    }
}