package com.lang.value;

public class BoolValue implements Value {
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
    public boolean valueEquals(Value value) {
        return value != null
                && value.isBool()
                && value.asBool().getValue() == this.value;
    }

    @Override
    public String valueToString() {
        return Boolean.toString(value);
    }

    public boolean getValue() {
        return value;
    }
}