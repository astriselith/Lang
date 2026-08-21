// com/lang/ast/Program.java
package com.lang.ast;

import com.lang.util.Position;
import java.util.List;

public class Program extends Node {
    public List<Expr> expressions;

    public Program() {}

    public Program(List<Expr> expressions, Position position) {
        super(position);
        this.expressions = expressions;
    }
}
