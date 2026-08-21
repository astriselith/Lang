package com.lang.parser;

public enum ParsingErrorCode {
    EXPECTED_TOKEN("Expected %s, found %s"),
    EXPECTED_TOKEN_AFTER("Exoected %s after %s"),
    UNEXPECTED_TOKEN("Unexpected token: %s"),
    UNEXPECTED_CONTENT("Unexpected content");

    public static final String TAG = "PARSER";

    private final String message;

    ParsingErrorCode(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        return String.format(message, args);
    }

    public String getMessage() {
        return message;
    }

    public String getTag() {
        return TAG;
    }
}