package com.lang.value;

import com.lang.ast.BlockExpr;

public abstract class Value {

    public static NullValue ofNull() {
        return NullValue.INSTANCE;
    }

    public static BoolValue ofBool(boolean value) {
        return BoolValue.of(value);
    }

    public static IntValue ofInt(long value) {
        return IntValue.of(value);
    }

    public static FloatValue ofFloat(double value) {
        return FloatValue.of(value);
    }

    public static StringValue ofString(String value) {
        return new StringValue(value);
    }

    public static BlockValue ofBlock(BlockExpr block, BlockValue parent) {
        return new BlockValue(block, parent);
    }

    public boolean isNull() {
        return this instanceof NullValue;
    }

    public boolean isBool() {
        return this instanceof BoolValue;
    }

    public boolean isInt() {
        return this instanceof IntValue;
    }

    public boolean isFloat() {
        return this instanceof FloatValue;
    }

    public boolean isString() {
        return this instanceof StringValue;
    }

    public boolean isBlock() {
        return this instanceof BlockValue;
    }

    public NullValue asNull() {
        if (this instanceof NullValue) return (NullValue) this;
        throw new RuntimeException(
                "Cannot convert " + getClass().getSimpleName() + " to NullValue");
    }

    public BoolValue asBool() {
        if (this instanceof BoolValue) return (BoolValue) this;
        throw new RuntimeException(
                "Cannot convert " + getClass().getSimpleName() + " to BoolValue");
    }

    public IntValue asInt() {
        if (this instanceof IntValue) return (IntValue) this;
        throw new RuntimeException("Cannot convert " + getClass().getSimpleName() + " to IntValue");
    }

    public FloatValue asFloat() {
        if (this instanceof FloatValue) return (FloatValue) this;
        throw new RuntimeException(
                "Cannot convert " + getClass().getSimpleName() + " to FloatValue");
    }

    public StringValue asString() {
        if (this instanceof StringValue) return (StringValue) this;
        throw new RuntimeException(
                "Cannot convert " + getClass().getSimpleName() + " to StringValue");
    }

    public BlockValue asBlock() {
        if (this instanceof BlockValue) return (BlockValue) this;
        throw new RuntimeException(
                "Cannot convert " + getClass().getSimpleName() + " to BlockValue");
    }

    public boolean equalsNull(Value value) {
        return false;
    }

    public boolean equalsBool(Value value) {
        return false;
    }

    public boolean equalsInt(Value value) {
        return false;
    }

    public boolean equalsFloat(Value value) {
        return false;
    }

    public boolean equalsString(Value value) {
        return false;
    }

    public boolean equalsBlock(Value value) {
        return false;
    }

    public boolean equalsLBool(boolean value) {
        return false;
    }

    public boolean equalsLInt(long value) {
        return false;
    }

    public boolean equalsLFloat(double value) {
        return false;
    }

    public boolean equalsLString(String value) {
        return false;
    }

    public boolean toLBool() {
        return false;
    }

    public long toLInt() {
        return 0;
    }

    public double toLFloat() {
        return 0.0;
    }

    public String toLString() {
        return "";
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;

        if (other instanceof Value) {
            Value value = (Value) other;

            if (isNull()) return equalsNull(value);
            if (isBool()) return equalsBool(value);
            if (isInt()) return equalsInt(value);
            if (isFloat()) return equalsFloat(value);
            if (isString()) return equalsString(value);
            if (isBlock()) return equalsBlock(value);
        }

        return false;
    }

    @Override
    public final String toString() {
        return toLString();
    }
}
