package com.lang.ast;

import com.lang.util.Position;

public class ParenthesizedExpr extends Expr {
    public Expr inner;

    public ParenthesizedExpr(Expr inner, Position position) {
        super(position);
        this.inner = inner;
    }

    @Override
    public Object accept(ExprVisitor visitor) {
        return visitor.visitParenthesizedExpr(this);
    }
}
