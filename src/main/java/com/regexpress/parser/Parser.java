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
			else if (tryConsume('{')) node = parseBounds(node);
			else break;
		}
		return node;
	}

	private Node parseBounds(Node node) {
		int min = parseNumber();
		int max = min;
		boolean unbounded = false;

		if (tryConsume(',')) {
			if (hasMore() && peek() != '}') {
				max = parseNumber();
			} else {
				unbounded = true;
			}
		}

		if (!unbounded && max < min) {
			throw error("range " + min + " to " + max + " is invalid");
		}

		expect('}');

		Node required = repeat(node, min);

		if (unbounded) {
			if (min == 0) return new StarNode(node);
			if (min == 1) return new PlusNode(node);
			return new ConcatNode(required, new StarNode(node));
		}

		if (max == min) return required;
		if (min == 0 && max == 1) return new OptionalNode(node);

		return new ConcatNode(required, atMost(node, max - min));
	}

	private static Node repeat(Node node, int count) {
		if (count == 0) return new EmptyNode();
		Node result = node;
		for (int i = 1; i < count; i++) {
			result = new ConcatNode(result, node);
		}
		return result;
	}

	private static Node atMost(Node node, int count) {
		Node result = new OptionalNode(node);
		for (int i = 1; i < count; i++) {
			result = new OptionalNode(new ConcatNode(node, result));
		}
		return result;
	}

	private int parseNumber() {
		int start = position;
		int number = 0;
		while (hasMore() && peek() != ',' && peek() != '}') {
			char c = consume();
			if (!(c >= '0' && c <= '9')) throw error("expected a digit");
			number = number * 10 + (c - '0');
		}
		if (position == start) throw error("expected a digit");
		return number;
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
