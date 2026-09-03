package com.lang.value;

public class NullValue implements Value {
    public static final NullValue INSTANCE = new NullValue();

    private NullValue() {}

    @Override
    public boolean valueEquals(Value value) {
        return value != null && value.isNull();
    }

    @Override
    public String valueToString() {
        return "null";
    }
}