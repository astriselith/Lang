package com.lang.value;

public class IntValue implements Value {
    private static final int CACHE_MIN = -128;
    private static final int CACHE_MAX = 127;
    private static final IntValue[] CACHE = new IntValue[CACHE_MAX - CACHE_MIN + 1];

    static {
        for (int i = CACHE_MIN; i <= CACHE_MAX; i++) {
            CACHE[i - CACHE_MIN] = new IntValue(i);
        }
    }

    private final long value;

    private IntValue(long value) {
        this.value = value;
    }

    public static IntValue of(long value) {
        if (value >= CACHE_MIN && value <= CACHE_MAX) {
            return CACHE[(int) value - CACHE_MIN];
        }
        return new IntValue(value);
    }

    @Override
    public boolean valueEquals(Value value) {
        return value != null && value.isInt() && value.asInt().getValue() == this.value;
    }

    @Override
    public String valueToString() {
        return Long.toString(value);
    }

    public long getValue() {
        return value;
    }
}
