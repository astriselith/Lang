package com.lang.unit;

import com.lang.util.Position;
import com.lang.util.Positioned;

public class WarningException extends Exception {
    private final String message;
    private final Position position;

    public WarningException(String message, Position position) {
        super(formatMessage(message, position));
        this.message = message;
        this.position = position;
    }

    public WarningException(String message, Positioned positioned) {
        this(message, positioned != null ? positioned.getPosition() : null);
    }

    public WarningException(String message) {
        super(message);
        this.message = message;
        this.position = null;
    }

    private static String formatMessage(String message, Position position) {
        if (position == null) return message;
        return String.format("%s [line %d, column %d]", message, position.getLine(), position.getColumn());
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}