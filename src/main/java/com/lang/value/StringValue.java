package com.lang.value;

public class StringValue extends BlockValue {
    private final String value;

    public StringValue(String value) {
        this.value = value;

        this.setLocal("length", (CallableValue) (rt, arguments, bindings) -> {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("length() expects 0 arguments");
            }

            return ValueResult.of(ValueResult.NORMAL, null, Value.ofInt(value.length()));
        });

        this.setLocal("charAt", (CallableValue) (rt, arguments, bindings) -> {
            if (arguments.size() != 1) {
                throw new RuntimeException("charAt(index) expects 1 argument");
            }

            ValueResult indexResult = rt.execute(arguments.get(0));
            if (indexResult.isLaunched()) {
                return indexResult;
            }

            Value indexValue = indexResult.getValue();
            if (!indexValue.isInt()) {
                throw new RuntimeException("charAt() index must be an integer");
            }

            long index = indexValue.asInt().getValue();

            if (index < 0 || index >= value.length()) {
                throw new RuntimeException("String index out of bounds: " + index);
            }

            return ValueResult.of(ValueResult.NORMAL, null,
                    Value.ofString(String.valueOf(value.charAt((int) index))));
        });

        this.setLocal("substring", (CallableValue) (rt, arguments, bindings) -> {
            if (arguments.size() != 2) {
                throw new RuntimeException("substring(start, end) expects 2 arguments");
            }

            ValueResult startResult = rt.execute(arguments.get(0));
            if (startResult.isLaunched()) {
                return startResult;
            }

            ValueResult endResult = rt.execute(arguments.get(1));
            if (endResult.isLaunched()) {
                return endResult;
            }

            Value startValue = startResult.getValue();
            Value endValue = endResult.getValue();

            if (!startValue.isInt() || !endValue.isInt()) {
                throw new RuntimeException("substring() arguments must be integers");
            }

            long start = startValue.asInt().getValue();
            long end = endValue.asInt().getValue();

            if (start < 0 || start > value.length() || end < 0 || end > value.length() || start > end) {
                throw new RuntimeException("Invalid substring range: " + start + " to " + end);
            }

            return ValueResult.of(ValueResult.NORMAL, null,
                    Value.ofString(value.substring((int) start, (int) end)));
        });

        this.setLocal("toUpperCase", (CallableValue) (rt, arguments, bindings) -> {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("toUpperCase() expects 0 arguments");
            }

            return ValueResult.of(ValueResult.NORMAL, null,
                    Value.ofString(value.toUpperCase()));
        });

        this.setLocal("toLowerCase", (CallableValue) (rt, arguments, bindings) -> {
            if (!arguments.isEmpty()) {
                throw new RuntimeException("toLowerCase() expects 0 arguments");
            }

            return ValueResult.of(ValueResult.NORMAL, null,
                    Value.ofString(value.toLowerCase()));
        });
    }

    @Override
    public boolean valueEquals(Value value) {
        return value != null && value.isString() && value.asString().getValue().equals(this.value);
    }

    @Override
    public String valueToString() {
        return value;
    }

    public String getValue() {
        return value;
    }
}