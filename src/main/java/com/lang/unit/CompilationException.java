package com.lang.unit;

import com.lang.util.Position;
import com.lang.util.Positioned;

public class CompilationException extends RuntimeException {
    private final Position position;

    public CompilationException(String message, Position position) {
        super(formatMessage(message, position));
        this.position = position;
    }

    public CompilationException(String message, Positioned positioned) {
        this(message, positioned != null ? positioned.getPosition() : null);
    }

    public CompilationException(String message) {
        super(message);
        this.position = null;
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
        this.position = null;
    }

    private static String formatMessage(String message, Position position) {
        if (position == null) return message;
        return String.format("%s [line %d, column %d]", message, position.getLine(), position.getColumn());
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}