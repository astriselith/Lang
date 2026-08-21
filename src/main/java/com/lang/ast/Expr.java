package com.lang.ast;

import com.lang.util.Position;

public abstract class Expr extends Node {
    public Expr() {
    }

    public Expr(Position position) {
        super(position);
    }

    public abstract Object accept(ExprVisitor visitor);
}