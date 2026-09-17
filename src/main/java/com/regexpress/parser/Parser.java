package com.regexpress.parser;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.CharNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.EmptyNode;
import com.regexpress.ast.Node;
import com.regexpress.ast.OptionalNode;
import com.regexpress.ast.PlusNode;
import com.regexpress.ast.StarNode;

public final class Parser {

	private final String pattern;
	private int position;

	private Parser(String pattern) {
		this.pattern = pattern;
	}

	public static Node parse(String pattern) {
		Parser parser = new Parser(pattern);
		Node node = parser.parseAlternation();
		parser.expectEndOfInput();
		return node;
	}

	private void expectEndOfInput() {
		if (position != pattern.length()) {
			throw error("expected end of input");
		}
	}

	private RegexSyntaxException error(String message) {
		return new RegexSyntaxException(message, pattern, position);
	}

	private boolean hasMore() {
		return position < pattern.length();
	}

	private char peek() {
		return pattern.charAt(position);
	}

	private char consume() {
		return pattern.charAt(position++);
	}

	private boolean tryConsume(char c) {
		if (hasMore() && peek() == c) {
			position++;
			return true;
		}
		return false;
	}

	private void expect(char c) {
		if (!tryConsume(c)) {
			throw error("expected '" + c + "'");
		}
	}

	private Node parseAlternation() {
		Node left = parseConcatenation();

		while (tryConsume('|')) {
			Node right = parseConcatenation();
			left = new AlternateNode(left, right);
		}
		return left;
	}

	private Node parseConcatenation() {
		if (!hasMoreItems()) {
			return new EmptyNode();
		}

		Node left = parseRepetition();

		while (hasMoreItems()) {
			Node right = parseRepetition();
			left = new ConcatNode(left, right);
		}
		return left;
	}

	private boolean hasMoreItems() {
		return hasMore() && peek() != '|' && peek() != ')';
	}

	private Node parseRepetition() {
		Node node = parseAtom();

		while (hasMore()) {
			if (tryConsume('*')) node = new StarNode(node);
			else if (tryConsume('+')) node = new PlusNode(node);
			else if (tryConsume('?')) node = new OptionalNode(node);
			else break;
		}
		return node;
	}

	private Node parseAtom() {
		if (!hasMore()) {
			throw error("unexpected end of pattern");
		}

		if (tryConsume('(')) {
			Node inner = parseAlternation();
			expect(')');
			return inner;
		}

		char c = consume();
		if(c == '*' || c == '+' || c == '?' || c == '|' || c == ')') {
			throw error("unexpected '" + c + "'");
		}
		return new CharNode(c);
	}
}
