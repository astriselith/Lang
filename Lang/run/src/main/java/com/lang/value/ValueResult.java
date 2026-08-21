package com.lang.value;

public class ValueResult {
    private final int kind;
    private final String id;
    private final Value value;

    public static final int NORMAL = 0;
    public static final int LAUNCHED = 1;

    private ValueResult(int kind, String id, Value value) {
        this.kind = kind;
        this.id = id;
        this.value = value;
    }

    public static ValueResult of(int kind, String id, Value value) {
        return new ValueResult(kind, id, value);
    }

    public boolean isNormal() {
        return kind == NORMAL;
    }

    public boolean isLaunched() {
        return kind == LAUNCHED;
    }

    public int getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public Value getValue() {
        return value;
    }
}