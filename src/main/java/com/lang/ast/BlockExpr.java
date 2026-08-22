package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class BlockExpr extends Expr {
    public List<RefExpr> parameters;
    public List<RefExpr> attachments;
    public List<Expr> expressions;
    public boolean hasParens;
    public boolean hasColon;
    public boolean hasArrow;
    public boolean isSingleExpr;

    public BlockExpr() {
    }

    public BlockExpr(List<RefExpr> parameters, List<RefExpr> attachments, List<Expr> expressions,
            boolean hasParens, boolean hasColon, boolean hasArrow, boolean isSingleExpr,
            Position position) {
        super(position);
        this.parameters = parameters;
        this.attachments = attachments;
        this.expressions = expressions;
        this.hasParens = hasParens;
        this.hasColon = hasColon;
        this.hasArrow = hasArrow;
        this.isSingleExpr = isSingleExpr;
    }

    @Override
    public Object accept(ExprVisitor visitor) {
        return visitor.visitBlockExpr(this);
    }
}