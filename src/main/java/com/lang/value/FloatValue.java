package com.lang.value;

public class FloatValue extends Value {
    private static final FloatValue ZERO = new FloatValue(0.0);
    private static final FloatValue ONE = new FloatValue(1.0);
    private static final FloatValue MINUS_ONE = new FloatValue(-1.0);
    private static final FloatValue TWO = new FloatValue(2.0);
    private static final FloatValue HALF = new FloatValue(0.5);
    private static final FloatValue PI = new FloatValue(Math.PI);
    private static final FloatValue E = new FloatValue(Math.E);

    private final double value;

    private FloatValue(double value) {
        this.value = value;
    }

    public static FloatValue of(double value) {
        if (value == 0.0) return ZERO;
        if (value == 1.0) return ONE;
        if (value == -1.0) return MINUS_ONE;
        if (value == 2.0) return TWO;
        if (value == 0.5) return HALF;
        if (value == Math.PI) return PI;
        if (value == Math.E) return E;
        return new FloatValue(value);
    }

    @Override
    public boolean equalsFloat(Value value) {
        return value != null && value.isFloat() && equalsLFloat(value.toLFloat());
    }

    @Override
    public boolean equalsLFloat(double value) {
        return Double.compare(this.value, value) == 0;
    }

    @Override
    public long toLInt() {
        return (long) value;
    }

    @Override
    public double toLFloat() {
        return value;
    }

    @Override
    public String toLString() {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    public double getValue() {
        return value;
    }
}
