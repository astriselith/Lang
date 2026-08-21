package com.lang.value;

public class NullValue extends Value {
    public static final NullValue INSTANCE = new NullValue();

    private NullValue() {}

    @Override
    public boolean equalsNull(Value value) {
        return value != null && value.isNull();
    }

    @Override
    public String toLString() {
        return "null";
    }
}