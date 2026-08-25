package com.lang.runtime;

import com.lang.ast.Expr;
import com.lang.ast.ExprVisitor;
import com.lang.ast.Program;
import com.lang.value.BlockValue;
import com.lang.value.Value;
import com.lang.value.ValueResult;

public interface Runtime extends ExprVisitor {

    String workingDir();

    ValueResult execute(Program program, BlockValue initScope);

    ValueResult accept(Expr expr);

    void pushScope(BlockValue scope);

    BlockValue popScope();

    BlockValue peekScope();

    void pushValue(Value value);

    Value popValue();

    Value peekValue();
}