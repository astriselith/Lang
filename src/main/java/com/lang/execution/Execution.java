package com.lang.execution;

import com.lang.ast.Expr;
import com.lang.ast.ExprVisitor;
import com.lang.ast.Program;
import com.lang.value.BlockValue;
import com.lang.value.ValueResult;

public interface Execution extends ExprVisitor {

    String workingDir();

    ValueResult execute(Program program, BlockValue initScope);

    ValueResult execute(Expr expr);

    void pushScope(BlockValue scope);

    BlockValue popScope();

    BlockValue peekScope();
}