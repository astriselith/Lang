package com.lang.value;

import com.lang.ast.BlockExpr;

public interface Value {

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

    default boolean isNull() {
        return this instanceof NullValue;
    }

    default boolean isBool() {
        return this instanceof BoolValue;
    }

    default boolean isInt() {
        return this instanceof IntValue;
    }

    default boolean isFloat() {
        return this instanceof FloatValue;
    }

    default boolean isString() {
        return this instanceof StringValue;
    }

    default boolean isBlock() {
        return this instanceof BlockValue;
    }

    default boolean isCallable() {
        return this instanceof CallableValue;
    }

    default BoolValue asBool() {
        return (BoolValue) this;
    }

    default IntValue asInt() {
        return (IntValue) this;
    }

    default FloatValue asFloat() {
        return (FloatValue) this;
    }

    default StringValue asString() {
        return (StringValue) this;
    }

    default BlockValue asBlock() {
        return (BlockValue) this;
    }

    default CallableValue asCallable() {
        return (CallableValue) this;
    }

    default boolean valueEquals(Value other) {
        return false;
    }

    default String valueToString() {
        return null;
    }
}
