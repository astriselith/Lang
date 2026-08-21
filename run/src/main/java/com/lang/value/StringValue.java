package com.lang.value;

import com.lang.ast.Expr;
import com.lang.context.Context;

import java.util.List;

public class StringValue extends BlockValue {
    private final String value;

    public StringValue(String value) {
        this.value = value;

        this.set("length", new BlockValue(null, this) {

            @Override
            public ValueResult call(
                    Context ctx,
                    List<Expr> arguments,
                    List<Expr> bindings) {
                if (!arguments.isEmpty()) {
                    throw new RuntimeException("length() expects 0 arguments");
                }

                return ValueResult.of(ValueResult.NORMAL, null, Value.ofInt(value.length()));
            }
        });

        this.set("charAt", new BlockValue(null, this) {

            @Override
            public ValueResult call(
                    Context ctx,
                    List<Expr> arguments,
                    List<Expr> bindings) {
                if (arguments.size() != 1) {
                    throw new RuntimeException("charAt(index) expects 1 argument");
                }

                ValueResult indexResult = (ValueResult) arguments.get(0).accept(ctx.visitor());
                if (indexResult.isLaunched()) {
                    return indexResult;
                }

                Value indexValue = indexResult.getValue();
                if (!indexValue.isInt()) {
                    throw new RuntimeException("charAt() index must be an integer");
                }

                long index = indexValue.toLInt();

                if (index < 0 || index >= value.length()) {
                    throw new RuntimeException("String index out of bounds: " + index);
                }

                return ValueResult.of(ValueResult.NORMAL, null,
                        Value.ofString(String.valueOf(value.charAt((int) index))));
            }
        });

        this.set("substring", new BlockValue(null, this) {

            @Override
            public ValueResult call(
                    Context ctx,
                    List<Expr> arguments,
                    List<Expr> bindings) {
                if (arguments.size() != 2) {
                    throw new RuntimeException("substring(start, end) expects 2 arguments");
                }

                ValueResult startResult = (ValueResult) arguments.get(0).accept(ctx.visitor());
                if (startResult.isLaunched()) {
                    return startResult;
                }

                ValueResult endResult = (ValueResult) arguments.get(1).accept(ctx.visitor());
                if (endResult.isLaunched()) {
                    return endResult;
                }

                Value startValue = startResult.getValue();
                Value endValue = endResult.getValue();

                if (!startValue.isInt() || !endValue.isInt()) {
                    throw new RuntimeException("substring() arguments must be integers");
                }

                long start = startValue.toLInt();
                long end = endValue.toLInt();

                if (start < 0 || start > value.length() || end < 0 || end > value.length() || start > end) {
                    throw new RuntimeException("Invalid substring range: " + start + " to " + end);
                }

                return ValueResult.of(ValueResult.NORMAL, null,
                        Value.ofString(value.substring((int) start, (int) end)));
            }
        });

        this.set("toUpperCase", new BlockValue(null, this) {

            @Override
            public ValueResult call(
                    Context ctx,
                    List<Expr> arguments,
                    List<Expr> bindings) {
                if (!arguments.isEmpty()) {
                    throw new RuntimeException("toUpperCase() expects 0 arguments");
                }

                return ValueResult.of(ValueResult.NORMAL, null,
                        Value.ofString(value.toUpperCase()));
            }
        });

        this.set("toLowerCase", new BlockValue(null, this) {

            @Override
            public ValueResult call(
                    Context ctx,
                    List<Expr> arguments,
                    List<Expr> bindings) {
                if (!arguments.isEmpty()) {
                    throw new RuntimeException("toLowerCase() expects 0 arguments");
                }

                return ValueResult.of(ValueResult.NORMAL, null,
                        Value.ofString(value.toLowerCase()));
            }
        });
    }

    @Override
    public boolean equalsString(Value value) {
        return value != null && value.isString() && equalsLString(value.toLString());
    }

    @Override
    public boolean equalsLString(String value) {
        return this.value.equals(value);
    }

    @Override
    public String toLString() {
        return value;
    }

    public String getValue() {
        return value;
    }
}