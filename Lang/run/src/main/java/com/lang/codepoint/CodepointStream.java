package com.lang.codepoint;

import com.lang.buffer.ObjectBuffer;
import java.io.IOException;

public abstract class CodepointStream extends ObjectBuffer {

    protected CodepointStream() {
        this(10);
    }

    protected CodepointStream(int sideWindow) {
        super(sideWindow);
    }

    @Override
    protected abstract Object fetchNext();

    @Override
    protected boolean isEOF(Object codepoint) {
        return codepoint == null || (Integer) codepoint == Codepoint.EOF;
    }

    public int peek() {
        Object value = offset(0);
        return value != null ? (Integer) value : Codepoint.EOF;
    }

    public int peekNext() {
        Object value = offset(1);
        return value != null ? (Integer) value : Codepoint.EOF;
    }

    public int peekNextNext() {
        Object value = offset(2);
        return value != null ? (Integer) value : Codepoint.EOF;
    }

    public int peekBack() {
        Object value = offset(-1);
        return value != null ? (Integer) value : Codepoint.EOF;
    }

    public int peekBackBack() {
        Object value = offset(-2);
        return value != null ? (Integer) value : Codepoint.EOF;
    }

    public int advance() {
        Object current = offset(0);
        if (current != null && (Integer) current != Codepoint.EOF) {
            next();
            return (Integer) current;
        }
        return Codepoint.EOF;
    }

    public boolean check(int value) {
        Object current = offset(0);
        return current != null && (Integer) current == value;
    }

    public boolean checkIndex(int index, int value) {
        Object current = offset(index);
        return current != null && (Integer) current == value;
    }

    public boolean checkNext(int value) {
        return checkIndex(1, value);
    }

    public boolean checkNextNext(int value) {
        return checkIndex(2, value);
    }

    public boolean checkBack(int value) {
        return checkIndex(-1, value);
    }

    public boolean checkBackBack(int value) {
        return checkIndex(-2, value);
    }

    public boolean checkAny(int... values) {
        Object current = offset(0);
        if (current == null) return false;
        for (int v : values) {
            if ((Integer) current == v) return true;
        }
        return false;
    }

    public boolean checkIndexAny(int index, int... values) {
        Object current = offset(index);
        if (current == null) return false;
        for (int v : values) {
            if ((Integer) current == v) return true;
        }
        return false;
    }

    public boolean checkNextAny(int... values) {
        return checkIndexAny(1, values);
    }

    public boolean checkNextNextAny(int... values) {
        return checkIndexAny(2, values);
    }

    public boolean checkBackAny(int... values) {
        return checkIndexAny(-1, values);
    }

    public boolean checkBackBackAny(int... values) {
        return checkIndexAny(-2, values);
    }

    public boolean checkSequence(int... values) {
        if (values == null || values.length == 0) return true;
        for (int i = 0; i < values.length; i++) {
            if (!checkIndex(i, values[i])) return false;
        }
        return true;
    }

    public boolean checkSequenceIndex(int index, int... values) {
        if (values == null || values.length == 0) return true;
        for (int i = 0; i < values.length; i++) {
            if (!checkIndex(index + i, values[i])) return false;
        }
        return true;
    }

    public boolean checkNextSequence(int... values) {
        return checkSequenceIndex(1, values);
    }

    public boolean checkBackSequence(int... values) {
        return checkSequenceIndex(-1, values);
    }

    public boolean match(int value) {
        if (check(value)) {
            next();
            return true;
        }
        return false;
    }

    public boolean matchAny(int... values) {
        for (int v : values) {
            if (check(v)) {
                next();
                return true;
            }
        }
        return false;
    }

    public boolean matchSequence(int... values) {
        if (values == null || values.length == 0) return true;
        if (!checkSequence(values)) return false;
        for (int i = 0; i < values.length; i++) next();
        return true;
    }

    public int expect(int value) {
        int current = peek();
        if (current != value) {
            throw new IllegalStateException("Expected '" + Codepoint.toString(value) + "' but found '" + Codepoint.toString(current) + "'");
        }
        advance();
        return current;
    }

    public int expectAny(int... values) {
        int current = peek();
        for (int value : values) {
            if (current == value) {
                advance();
                return current;
            }
        }
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) expected.append(", ");
            expected.append("'").append(Codepoint.toString(values[i])).append("'");
        }
        throw new IllegalStateException("Expected one of [" + expected + "] but found '" + Codepoint.toString(current) + "'");
    }

    public int[] expectSequence(int... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one value must be provided");
        }
        if (!checkSequence(values)) {
            int current = peek();
            throw new IllegalStateException("Expected sequence starting with " + Codepoint.toString(values[0]) + " but found " + Codepoint.toString(current));
        }
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = advance();
        }
        return result;
    }

    public boolean isAtEnd() {
        return !hasNext();
    }

    public void reset() {
        release();
    }

    public abstract void close() throws IOException;
}