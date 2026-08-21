package com.lang.value;

public class BoolValue extends Value {
    public static final BoolValue TRUE = new BoolValue(true);
    public static final BoolValue FALSE = new BoolValue(false);

    private final boolean value;

    private BoolValue(boolean value) {
        this.value = value;
    }

    public static BoolValue of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public boolean equalsBool(Value value) {
        return value != null
                && value.isBool()
                && equalsLBool(value.toLBool());
    }

    @Override
    public boolean equalsLBool(boolean value) {
        return this.value == value;
    }

    @Override
    public boolean toLBool() {
        return value;
    }

    @Override
    public String toLString() {
        return Boolean.toString(value);
    }

    public boolean getValue() {
        return value;
    }
}