package com.lang.ast;

import com.lang.util.Position;

public class Param extends Node {
    public Identifier name;
    
    public Param() {
    }

    public Param(Identifier name, Position position) {
        super(position);
        this.name = name;
    }
}