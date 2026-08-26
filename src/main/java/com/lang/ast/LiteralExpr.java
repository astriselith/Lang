package com.lang.ast;

import com.lang.util.Position;

public class LiteralExpr extends Expr {
    public static final int NULL = 0,
            BOOL = 1,
            INT = 2,
            FLOAT = 3,
            STRING = 4;

    public int type;
    public String lexeme;

    public LiteralExpr() {
    }

    public LiteralExpr(int type, String lexeme, Position position) {
        super(position);
        this.type = type;
        this.lexeme = lexeme;
    }

    @Override
    public Object accept(ExprVisitor visitor) {
        return visitor.visitLiteralExpr(this);
    }
}