package com.lang.token;

import com.lang.buffer.ObjectBuffer;

public abstract class TokenStream extends ObjectBuffer {

    protected TokenStream() {
        this(10);
    }

    protected TokenStream(int sideWindow) {
        super(sideWindow);
    }

    @Override
    protected abstract Object fetchNext();

    @Override
    protected boolean isEOF(Object token) {
        return token == null || ((Token) token).isEof();
    }

    public Token peek() {
        return (Token) offset(0);
    }

    public Token peekNext() {
        return (Token) offset(1);
    }

    public Token peekNextNext() {
        return (Token) offset(2);
    }

    public Token peekBack() {
        return (Token) offset(-1);
    }

    public Token peekBackBack() {
        return (Token) offset(-2);
    }

    public Token advance() {
        return (Token) next();
    }

    public Token previous() {
        return (Token) offset(-1);
    }

    public boolean check(int type) {
        Token t = (Token) offset(0);
        return t != null && t.type == type;
    }

    public boolean checkIndex(int index, int type) {
        Token t = (Token) offset(index);
        return t != null && t.type == type;
    }

    public boolean checkNext(int type) {
        return checkIndex(1, type);
    }

    public boolean checkNextNext(int type) {
        return checkIndex(2, type);
    }

    public boolean checkBack(int type) {
        return checkIndex(-1, type);
    }

    public boolean checkBackBack(int type) {
        return checkIndex(-2, type);
    }

    public boolean checkAny(int... types) {
        if (types == null || types.length == 0)
            return false;
        Token t = (Token) offset(0);
        if (t == null)
            return false;
        for (int type : types) {
            if (t.type == type)
                return true;
        }
        return false;
    }

    public boolean checkIndexAny(int index, int... types) {
        if (types == null || types.length == 0)
            return false;
        Token t = (Token) offset(index);
        if (t == null)
            return false;
        for (int type : types) {
            if (t.type == type)
                return true;
        }
        return false;
    }

    public boolean checkNextAny(int... types) {
        return checkIndexAny(1, types);
    }

    public boolean checkNextNextAny(int... types) {
        return checkIndexAny(2, types);
    }

    public boolean checkBackAny(int... types) {
        return checkIndexAny(-1, types);
    }

    public boolean checkBackBackAny(int... types) {
        return checkIndexAny(-2, types);
    }

    public boolean checkSequence(int... types) {
        if (types == null || types.length == 0)
            return true;
        for (int i = 0; i < types.length; i++) {
            if (!checkIndex(i, types[i]))
                return false;
        }
        return true;
    }

    public boolean checkSequenceIndex(int index, int... types) {
        if (types == null || types.length == 0)
            return true;
        for (int i = 0; i < types.length; i++) {
            if (!checkIndex(index + i, types[i]))
                return false;
        }
        return true;
    }

    public boolean checkNextSequence(int... types) {
        return checkSequenceIndex(1, types);
    }

    public boolean checkBackSequence(int... types) {
        return checkSequenceIndex(-1, types);
    }

    public boolean match(int type) {
        if (check(type)) {
            next();
            return true;
        }
        return false;
    }

    public boolean matchAny(int... types) {
        if (types == null || types.length == 0)
            return false;
        for (int type : types) {
            if (check(type)) {
                next();
                return true;
            }
        }
        return false;
    }

    public boolean matchSequence(int... types) {
        if (types == null || types.length == 0)
            return true;
        if (!checkSequence(types))
            return false;
        for (int i = 0; i < types.length; i++)
            next();
        return true;
    }

    public Token expect(int type) {
        Token token = (Token) offset(0);
        if (token == null || token.type != type) {
            throw new IllegalStateException("Expected " + Token.typeName(type) + " but found "
                    + (token != null ? Token.typeName(token.type) : "EOF") + " at " + token.position.toString());
        }
        next();
        return token;
    }

    public Token expectAny(int... types) {
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException("At least one type must be provided");
        }
        Token token = (Token) offset(0);
        if (token != null) {
            for (int type : types) {
                if (token.type == type) {
                    next();
                    return token;
                }
            }
        }
        throw new IllegalStateException("Unexpected token: " + (token != null ? Token.typeName(token.type) : "EOF")
                + " at " + token.position.toString());
    }

    public Token[] expectSequence(int... types) {
        if (types == null || types.length == 0) {
            throw new IllegalArgumentException("At least one type must be provided");
        }
        if (!checkSequence(types)) {
            Token token = (Token) offset(0);
            throw new IllegalStateException("Expected sequence starting with " + Token.typeName(types[0])
                    + " but found " + (token != null ? Token.typeName(token.type) : "EOF"));
        }
        Token[] tokens = new Token[types.length];
        for (int i = 0; i < types.length; i++) {
            tokens[i] = (Token) next();
        }
        return tokens;
    }

    public boolean isAtEnd() {
        return !hasNext();
    }

    public void reset() {
        release();
    }
}