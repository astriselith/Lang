package com.lang.ast;

public interface ExprVisitor {
    Object visitLiteralExpr(LiteralExpr expr);

    Object visitRefExpr(RefExpr expr);

    Object visitAssignExpr(AssignExpr expr);

    Object visitBinaryExpr(BinaryExpr expr);

    Object visitUnaryExpr(UnaryExpr expr);

    Object visitCallExpr(CallExpr expr);

    Object visitMemberAccessExpr(MemberAccessExpr expr);

    Object visitBlockExpr(BlockExpr expr);
}