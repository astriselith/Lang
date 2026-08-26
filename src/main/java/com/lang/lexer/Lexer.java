package com.lang.lexer;

import static com.lang.lexer.LexingErrorCode.*;
import static com.lang.token.Token.*;

import com.lang.token.*;
import com.lang.util.Position;
import com.lang.codepoint.CodepointStream;
import com.lang.codepoint.Codepoint;
import com.lang.unit.CompilationException;
import com.lang.unit.CompilationUnit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Lexer extends TokenStream {

    private final CodepointStream source;
    private final CompilationUnit unit;

    private int current = 0;
    private int line = 1;
    private int lineStart = 0;
    private int start = 0;

    private boolean eofReached = false;

    private final Map<String, Integer> keywords = new HashMap<>();
    private final Map<Integer, Integer> symbols = new HashMap<>();

    public Lexer(CodepointStream source) {
        this(source, null);
    }

    public Lexer(CodepointStream source, CompilationUnit unit) {
        super(10);

        this.source = source;
        this.unit = unit;

        keywords.put("true", BOOL);
        keywords.put("false", BOOL);
        keywords.put("null", NULL);

        symbols.put((int) '+', PLUS);
        symbols.put((int) '-', MINUS);
        symbols.put((int) '*', STAR);
        symbols.put((int) '/', SLASH);
        symbols.put((int) '%', PERCENT);
        symbols.put((int) '=', EQUALS);
        symbols.put((int) '!', BANG);
        symbols.put((int) '<', LANGLE);
        symbols.put((int) '>', RANGLE);
        symbols.put((int) '&', AMP);
        symbols.put((int) '|', BAR);
        symbols.put((int) '?', QUESTION);
        symbols.put((int) ':', COLON);
        symbols.put((int) '(', LPAREN);
        symbols.put((int) ')', RPAREN);
        symbols.put((int) '{', LBRACE);
        symbols.put((int) '}', RBRACE);
        symbols.put((int) '[', LBRACKET);
        symbols.put((int) ']', RBRACKET);
        symbols.put((int) ',', COMMA);
        symbols.put((int) '.', DOT);
        symbols.put((int) ';', SEMICOLON);
        symbols.put((int) '$', DOLLAR);
        symbols.put((int) '@', AT);
    }

    private void update() {
        int cp = source.advance();
        current++;

        if (cp == '\n') {
            line++;
            lineStart = current;
        }
    }

    private void update(int n) {
        for (int i = 0; i < n; i++) {
            update();
        }
    }

    private Position position() {
        return new Position(line, lineStart, start, current);
    }

    private Token undefined(String lexeme) {
        return Token.of(UNDEFINED, lexeme, line, lineStart, start, current);
    }

    @Override
    protected Object fetchNext() {
        while (true) {
            if (eofReached) {
                return Token.eof(line, lineStart, current, current);
            }

            if (source.isAtEnd()) {
                eofReached = true;
                return Token.eof(line, lineStart, current, current);
            }

            start = current;
            Token token = scan();

            if (token.isUndefined()) {
                continue;
            }

            return token;
        }
    }

    private Token scan() {
        skip();

        if (source.isAtEnd()) {
            eofReached = true;
            return Token.eof(line, lineStart, current, current);
        }

        int cp = source.peek();

        if (symbols.containsKey(cp)) {
            int type = symbols.get(cp);
            update();
            return Token.of(type, Codepoint.toString(cp), line, lineStart, start, current);
        }

        if (Codepoint.isDigit(cp)) {
            return number();
        }

        if (Codepoint.isIdentifierStart(cp)) {
            return identifier();
        }

        if (cp == '"') {
            return string();
        }

        String text = Codepoint.toString(cp);

        unit.addError(LexingErrorCode.TAG, new CompilationException(UNEXPECTED_CHARACTER.format(text), position()));

        update();
        return undefined(text);
    }

    private void skip() {
        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (Codepoint.isWhitespace(cp)) {
                update();
                continue;
            }

            if (cp != '#') {
                return;
            }

            int next = source.peekNext();

            if (next == '#' && source.peekNextNext() == '#') {
                update(3);

                boolean closed = false;

                while (!source.isAtEnd()) {
                    if (source.peek() == '#' && source.peekNext() == '#' && source.peekNextNext() == '#') {
                        update(3);
                        closed = true;
                        break;
                    }

                    if (source.peek() == '#') {
                        unit.addError(LexingErrorCode.TAG,
                                new CompilationException(MALFORMED_COMMENT.format(), position()));
                        update(1);
                        return;
                    }

                    update();
                }

                if (!closed) {
                    unit.addError(LexingErrorCode.TAG,
                            new CompilationException(UNTERMINATED_MULTILINE_COMMENT.format(), position()));
                }

                continue;
            }

            update(1);

            boolean closed = false;

            while (!source.isAtEnd()) {
                if (source.peek() == '#') {
                    int nextNext = source.peekNext();

                    if (nextNext == '#' && source.peekNextNext() == '#') {
                        unit.addError(LexingErrorCode.TAG,
                                new CompilationException(MALFORMED_COMMENT.format(), position()));
                        update(3);
                        return;
                    }

                    update(1);
                    closed = true;
                    break;
                }

                if (source.peek() == '\n') {
                    unit.addError(LexingErrorCode.TAG,
                            new CompilationException(UNTERMINATED_COMMENT.format(), position()));
                    update(1);
                    return;
                }

                update();
            }

            if (!closed) {
                unit.addError(LexingErrorCode.TAG,
                        new CompilationException(UNTERMINATED_COMMENT.format(), position()));
            }
        }
    }

    private Token identifier() {
        StringBuilder builder = new StringBuilder();

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (!Codepoint.isIdentifier(cp)) {
                break;
            }

            builder.appendCodePoint(cp);
            update();
        }

        String text = builder.toString();

        if (keywords.containsKey(text)) {
            int type = keywords.get(text);
            return Token.of(type, text, line, lineStart, start, current);
        }

        return Token.of(IDENTIFIER, text, line, lineStart, start, current);
    }

    private Token number() {
        StringBuilder builder = new StringBuilder();

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (Codepoint.isDigit(cp) || cp == '_' || cp == '.' || cp == 'e' || cp == 'E') {
                builder.appendCodePoint(cp);
                update();
            } else {
                break;
            }
        }

        String text = builder.toString();
        String clean = text.replace("_", "");

        if (clean.matches("^[0-9]+$")) {
            return Token.of(INT, text, line, lineStart, start, current);
        }

        if (clean.matches("^[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?$")) {
            return Token.of(FLOAT, text, line, lineStart, start, current);
        }

        unit.addError(LexingErrorCode.TAG, new CompilationException(INVALID_NUMBER.format(text), position()));

        return undefined(text);
    }

    private Token string() {
        StringBuilder lexeme = new StringBuilder();
        boolean isMultiLine = false;

        if (source.peek() == '"' && source.peekNext() == '"' && source.peekNextNext() == '"') {
            isMultiLine = true;
            lexeme.append("\"\"\"");
            update(3);

            if (source.peek() == '\n') {
                lexeme.append('\n');
                update();
            }
        } else {
            lexeme.append('"');
            update();
        }

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (isMultiLine) {
                if (cp == '"' && source.peekNext() == '"' && source.peekNextNext() == '"') {
                    lexeme.append("\"\"\"");
                    update(3);
                    return Token.of(MULTILINE_STRING, lexeme.toString(), line, lineStart, start, current);
                }
            } else {
                if (cp == '"') {
                    lexeme.append('"');
                    update();
                    return Token.of(STRING, lexeme.toString(), line, lineStart, start, current);
                }

                if (cp == '\n') {
                    unit.addError(LexingErrorCode.TAG,
                            new CompilationException(UNTERMINATED_STRING.format(), position()));
                    return undefined(lexeme.toString());
                }
            }

            if (cp == '\\') {
                lexeme.append('\\');
                update();

                if (source.isAtEnd()) {
                    break;
                }

                int next = source.peek();

                switch (next) {
                    case 'n':
                    case 'r':
                    case 't':
                    case '\\':
                    case '"':
                        lexeme.appendCodePoint(next);
                        update();
                        break;
                    default:
                        lexeme.appendCodePoint(next);
                        update();
                        unit.addError(LexingErrorCode.TAG, new CompilationException(
                                UNKNOWN_ESCAPE_SEQUENCE.format(Codepoint.toString(next)), position()));
                        return undefined(lexeme.toString());
                }

                continue;
            }

            lexeme.appendCodePoint(cp);
            update();
        }

        if (isMultiLine) {
            unit.addError(LexingErrorCode.TAG,
                    new CompilationException(UNTERMINATED_MULTILINE_STRING.format(), position()));
        } else {
            unit.addError(LexingErrorCode.TAG,
                    new CompilationException(UNTERMINATED_STRING.format(), position()));
        }
        return undefined(lexeme.toString());
    }

    public CompilationUnit getCompilationUnit() {
        return unit;
    }

    public void close() throws IOException {
        source.close();
    }
}