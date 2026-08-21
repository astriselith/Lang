package com.lang.context;

import java.util.Deque;

import com.lang.ast.ExprVisitor;
import com.lang.value.BlockValue;

public interface Context {
    Deque<BlockValue> stack();
    ExprVisitor visitor();
}
