package com.regexpress.parser;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.CharSetNode;
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

		if (tryConsume('[')) {
			Node charClass = buildCharSetNode();
			expect(']');
			return charClass;
		}

		char c = consume();
		if (c == '*' || c == '+' || c == '?' || c == '|' || c == ')' || c == ']') {
			throw error("unexpected '" + c + "'");
		}
		if (c == '.') return new CharSetNode(CharSet.all());
		return new CharSetNode(CharSet.of(c));
	}

	private Node buildCharSetNode() {
		CharSet set = CharSet.empty();
		if (tryConsume('^')) set.negate();

		while (hasMoreCharClassItems()) {
			char a = consume();
			boolean isRangeChar = tryConsume('-');

			if (isRangeChar && hasMoreCharClassItems()) {
				char b = consume();
				if (a > b) throw error("range " + a + " to " + b + " is invalid");
				set.union(CharSet.range(a, b));
			} else {
				set.union(CharSet.of(a));
				if (isRangeChar) set.union(CharSet.of('-'));
			}
		}
		return new CharSetNode(set);
	}

	private boolean hasMoreCharClassItems() {
		return hasMore() && peek() != ']';
	}
}
