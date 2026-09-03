package com.lang.value;

import java.util.List;

import com.lang.ast.Expr;
import com.lang.execution.Execution;

@FunctionalInterface
public interface CallableValue extends Value{
    ValueResult call(Execution rt, List<Expr> arguments, List<Expr> bindings);
}
