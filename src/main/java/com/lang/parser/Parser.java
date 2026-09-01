package com.lang.parser;

import static com.lang.parser.ParsingErrorCode.*;
import static com.lang.token.Token.*;

import com.lang.ast.*;
import com.lang.token.*;
import com.lang.unit.*;
import com.lang.util.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final TokenStream stream;
    private final CompilationUnit unit;

    public Parser(TokenStream stream, CompilationUnit unit) {
        this.stream = stream;
        this.unit = unit;
    }

    public CompilationUnit parse() {
        program();
        return unit;
    }

    private void program() {
        List<Expr> expressions = new ArrayList<>();

        while (!stream.isAtEnd() && !stream.check(EOF)) {
            try {

                expressions.add(expr());
                if (stream.check(SEMICOLON)) {
                    stream.advance();
                } else {
                    unit.addError(
                            TAG,
                            new CompilationException(
                                    EXPECTED_TOKEN_AFTER.format(";", "expression"), stream.previous()));
                }
            } catch (CompilationException exception) {
                unit.addError(TAG, exception);
                while (!stream.isAtEnd()
                        && !stream.check(EOF)
                        && !stream.check(SEMICOLON)) {
                    stream.advance();
                }
                if (stream.check(SEMICOLON)) {
                    stream.advance();
                }
            }
        }

        unit.setProgram(new Program(expressions, Position.ZERO));
    }

    private Position pos(Positioned p) {
        return p.getPosition();
    }

    private Position between(Positioned start, Positioned end) {
        return Position.between(start.getPosition(), end.getPosition());
    }

    private Expr expr() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logicalOr();

        if (stream.match(EQUALS)) {
            Expr value = assignment();
            Position pos = between(expr, value);
            return new AssignExpr(expr, new Operator("=", pos), value, pos);
        }

        return expr;
    }

    private Expr logicalOr() {
        Expr expr = logicalAnd();
        while (stream.matchSequence(BAR, BAR)) {
            Expr right = logicalAnd();
            Position pos = between(expr, right);
            expr = new BinaryExpr(expr, new Operator("||", pos), right, pos);
        }
        return expr;
    }

    private Expr logicalAnd() {
        Expr expr = equality();
        while (stream.matchSequence(AMP, AMP)) {
            Expr right = equality();
            Position pos = between(expr, right);
            expr = new BinaryExpr(expr, new Operator("&&", pos), right, pos);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (true) {
            if (stream.matchSequence(EQUALS, EQUALS)) {
                Expr right = comparison();
                Position pos = between(expr, right);
                expr = new BinaryExpr(expr, new Operator("==", pos), right, pos);
            } else if (stream.matchSequence(BANG, EQUALS)) {
                Expr right = comparison();
                Position pos = between(expr, right);
                expr = new BinaryExpr(expr, new Operator("!=", pos), right, pos);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (true) {
            if (stream.matchSequence(LANGLE, EQUALS)) {
                Expr right = term();
                Position pos = between(expr, right);
                expr = new BinaryExpr(expr, new Operator("<=", pos), right, pos);
            } else if (stream.matchSequence(RANGLE, EQUALS)) {
                Expr right = term();
                Position pos = between(expr, right);
                expr = new BinaryExpr(expr, new Operator(">=", pos), right, pos);
            } else if (stream.checkAny(LANGLE, RANGLE)) {
                Token op = stream.advance();
                Expr right = term();
                Position pos = between(expr, right);
                expr = new BinaryExpr(expr, new Operator(op.lexeme, pos), right, pos);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (stream.checkAny(PLUS, MINUS)) {
            Token op = stream.advance();
            Expr right = factor();
            Position pos = between(expr, right);
            expr = new BinaryExpr(expr, new Operator(op.lexeme, pos), right, pos);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (stream.checkAny(STAR, SLASH, PERCENT)) {
            Token op = stream.advance();
            Expr right = unary();
            Position pos = between(expr, right);
            expr = new BinaryExpr(expr, new Operator(op.lexeme, pos), right, pos);
        }
        return expr;
    }

    private Expr unary() {
        if (stream.checkAny(BANG, MINUS, PLUS)) {
            Token op = stream.advance();
            Expr operand = unary();
            Position pos = between(op, operand);
            return new UnaryExpr(new Operator(op.lexeme, pos), operand, pos);
        }

        return postfix();
    }

    private Expr postfix() {
        Expr expr = primary();

        while (!stream.isAtEnd()) {
            if (stream.checkAny(LPAREN, COLON)) {
                boolean hasParens = false;
                boolean hasColon = false;

                List<Expr> arguments = new ArrayList<>();
                List<Expr> bindings = new ArrayList<>();

                if (stream.match(LPAREN)) {
                    hasParens = true;

                    if (!stream.check(RPAREN)) {
                        do {
                            arguments.add(expr());
                        } while (stream.match(COMMA));
                    }

                    if (!stream.match(RPAREN)) {
                        throw new CompilationException(EXPECTED_TOKEN_AFTER.format(")", "("), stream.peek());
                    }

                }

                if (stream.match(COLON)) {
                    hasColon = true;

                    do {
                        bindings.add(expr());
                    } while (stream.match(BAR));
                }

                Positioned endPos = expr;

                if (!arguments.isEmpty()) {
                    endPos = arguments.get(arguments.size() - 1);
                }

                if (!bindings.isEmpty()) {
                    endPos = bindings.get(bindings.size() - 1);
                }

                expr = new CallExpr(expr, arguments, bindings, hasParens, hasColon, between(expr, endPos));

                continue;
            }

            if (stream.match(DOT)) {
                if (!stream.check(IDENTIFIER)) {
                    throw new CompilationException(EXPECTED_TOKEN.format(IDENTIFIER), stream.peek());
                }

                Token nameToken = stream.advance();

                expr = new MemberAccessExpr(expr, new Identifier(nameToken.lexeme, pos(nameToken)),
                        between(expr, nameToken));
                continue;
            }

            break;
        }

        return expr;
    }

    private Expr primary() {
        if (stream.checkAny(NULL, BOOL, INT, FLOAT, STRING)) {
            return literal();
        }

        if (stream.check(IDENTIFIER)) {
            return ref();
        }

        if (stream.checkAny(LPAREN, COLON, LBRACE)) {
            return block();
        }

        throw new CompilationException(UNEXPECTED_TOKEN.format(stream.peek()), stream.peek());
    }

    private LiteralExpr literal() {
        Token t = stream.advance();
        int literalType;

        switch (t.type) {
            case NULL:
                literalType = LiteralExpr.NULL;
                break;
            case BOOL:
                literalType = LiteralExpr.BOOL;
                break;
            case INT:
                literalType = LiteralExpr.INT;
                break;
            case FLOAT:
                literalType = LiteralExpr.FLOAT;
                break;
            case STRING:
                literalType = LiteralExpr.STRING;
                break;
            default:
                throw new IllegalStateException("Unexpected literal type: " + t.type);
        }

        return new LiteralExpr(literalType, t.lexeme, pos(t));
    }

    private RefExpr ref() {
        Token t = stream.advance();
        return new RefExpr(new Identifier(t.lexeme, pos(t)), pos(t));
    }

    private BlockExpr block() {
        Token start = stream.peek();

        boolean hasParens = false;
        boolean hasColon = false;
        boolean hasArrow = false;
        boolean isSingleExpr = false;

        List<Identifier> parameters = new ArrayList<>();
        List<Identifier> attachments = new ArrayList<>();

        if (stream.match(LPAREN)) {
            hasParens = true;
            while (!stream.isAtEnd() && !stream.check(RPAREN)) {
                if (!stream.check(IDENTIFIER)) {
                    throw new CompilationException(UNEXPECTED_TOKEN.format(stream.peek().lexeme), stream.peek());
                }
                Token paramToken = stream.advance();
                parameters.add(new Identifier(paramToken.lexeme, pos(paramToken)));
                if (!stream.match(COMMA)) {
                    break;
                }
            }
            if (!stream.check(RPAREN)) {
                throw new CompilationException(EXPECTED_TOKEN_AFTER.format(")", "("), stream.peek());
            }
            stream.advance();
        }

        if (stream.match(COLON)) {
            hasColon = true;
            while (!stream.isAtEnd()) {
                if (!stream.check(IDENTIFIER)) {
                    throw new CompilationException(UNEXPECTED_TOKEN.format(stream.peek().lexeme), stream.peek());
                }
                Token attachToken = stream.advance();
                attachments.add(new Identifier(attachToken.lexeme, pos(attachToken)));
                if (!stream.match(BAR)) {
                    break;
                }
            }
        }

        if (stream.matchSequence(MINUS, RANGLE)) {
            hasArrow = true;
        }

        List<Expr> expressions = new ArrayList<>();

        if (stream.match(LBRACE)) {
            while (!stream.isAtEnd() && !stream.check(RBRACE)) {
                try {
                    expressions.add(expr());
                    if (!stream.check(SEMICOLON)) {
                        throw new CompilationException(EXPECTED_TOKEN_AFTER.format(";", "expression"),
                                stream.previous());
                    }
                    stream.advance();
                } catch (CompilationException exception) {
                    unit.addError(TAG, exception);
                    while (!stream.isAtEnd() && !stream.check(EOF) &&
                            !stream.check(SEMICOLON) && !stream.check(RBRACE)) {
                        stream.advance();
                    }
                    if (stream.check(SEMICOLON)) {
                        stream.advance();
                    }
                }
            }
            if (!stream.check(RBRACE)) {
                throw new CompilationException(UNEXPECTED_TOKEN.format(stream.peek().lexeme), stream.peek());
            }
            stream.advance();
            isSingleExpr = false;
        } else {
            if (!hasArrow) {
                throw new CompilationException(UNEXPECTED_TOKEN.format(stream.peek().lexeme), stream.peek());
            }
            expressions.add(expr());
            isSingleExpr = true;
        }

        return new BlockExpr(
                parameters,
                attachments,
                expressions,
                hasParens,
                hasColon,
                hasArrow,
                isSingleExpr,
                between(start, stream.previous()));
    }

    public TokenStream getStream() {
        return stream;
    }

    public CompilationUnit getCompilationUnit() {
        return unit;
    }
}
