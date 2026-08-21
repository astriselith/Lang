package com.lang.ast;

import com.lang.util.Position;

public class LiteralExpr extends Expr {
    public static final int NULL = 0;
    public static final int BOOL = 1;
    public static final int INT = 2;
    public static final int FLOAT = 3;
    public static final int STRING = 4;

    public int type;
    public String value;

    public LiteralExpr() {
    }

    public LiteralExpr(int type, String value, Position position) {
        super(position);
        this.type = type;
        this.value = value;
    }

    @Override
    public Object accept(ExprVisitor visitor) {
        return visitor.visitLiteralExpr(this);
    }
}