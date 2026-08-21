package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class CallExpr extends Expr {
    public Expr callee;
    public List<Expr> arguments;
    public List<Expr> bindings;
    public boolean hasParens;
    public boolean hasColon;

    public CallExpr() {}

    public CallExpr(Expr callee, List<Expr> arguments, List<Expr> bindings, 
                    boolean hasParens, boolean hasColon, Position position) {
        super(position);
        this.callee = callee;
        this.arguments = arguments;
        this.bindings = bindings;
        this.hasParens = hasParens;
        this.hasColon = hasColon;
    }

    @Override
    public Object accept(ExprVisitor visitor) {
        return visitor.visitCallExpr(this);
    }
}