package com.lang.ast;
import com.lang.util.Position;

public class Attach extends Node {
    public Identifier name;

    public Attach() {}

    public Attach(Identifier name, Position position) {
        super(position);
        this.name = name;
    }
}
