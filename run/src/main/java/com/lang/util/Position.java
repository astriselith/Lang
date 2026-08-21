package com.lang.util;

public class Position {
    private final int line;
    private final int lineStart;
    private final int start;
    private final int end;

    public static final Position ZERO = new Position(0, 0, 0, 0);
    public static final Position UNKNOWN = new Position(-1, -1, -1, -1);

    public Position(int line, int lineStart, int start, int end) {
        this.line = line;
        this.lineStart = lineStart;
        this.start = start;
        this.end = end;
    }

    public int getLine() {
        return line;
    }

    public int getLineStart() {
        return lineStart;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getColumn() {
        return start - lineStart;
    }

    public int getColumnStart() {
        return start - lineStart;
    }

    public int getColumnEnd() {
        return end - lineStart;
    }

    public int length() {
        return end - start;
    }

    public static Position between(Position start, Position end) {
        return new Position(start.line, start.lineStart, start.start, end.end);
    }

    @Override
    public String toString() {
        return String.format("line %d, lineStart %d, pos %d-%d", line, lineStart, start, end);
    }
}
