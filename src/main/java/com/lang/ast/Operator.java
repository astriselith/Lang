package com.lang.ast;

import com.lang.util.Position;

public class Operator extends Node {
	public String lexeme;

	public Operator() {
	}

	public Operator(String lexeme, Position position) {
		super(position);
		this.lexeme = lexeme;
	}

	@Override
	public String toString() {
		return lexeme;
	}

}