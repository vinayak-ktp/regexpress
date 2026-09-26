package com.regexpress.parser;

import java.util.List;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.CharSetNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.EmptyNode;
import com.regexpress.ast.EndAnchorNode;
import com.regexpress.ast.GroupNode;
import com.regexpress.ast.Node;
import com.regexpress.ast.OptionalNode;
import com.regexpress.ast.PlusNode;
import com.regexpress.ast.StarNode;
import com.regexpress.ast.StartAnchorNode;
import com.regexpress.tokenizer.ClassShorthand;
import com.regexpress.tokenizer.End;
import com.regexpress.tokenizer.Literal;
import com.regexpress.tokenizer.Operator;
import com.regexpress.tokenizer.RegexSyntaxException;
import com.regexpress.tokenizer.Token;
import com.regexpress.tokenizer.Tokenizer;

public final class Parser {

	private final String pattern;
	private final List<Token> tokens;
	private int index;
	private int groupCount;

	private Parser(String pattern) {
		this.pattern = pattern;
		tokens = Tokenizer.tokenize(pattern);
	}

	public static Node parse(String pattern) {
		Parser parser = new Parser(pattern);
		Node node = parser.parseAlternation();
		parser.expectEndOfInput();
		return node;
	}

	private void expectEndOfInput() {
		if (hasMore()) throw error("expected end of input");
	}

	private boolean hasMore() {
		return !(current() instanceof End);
	}

	private RegexSyntaxException error(String message) {
		return new RegexSyntaxException(message, pattern, current().position());
	}

	private Token current() {
		return tokens.get(index);
	}

	private char consumeLiteral() {
		if (current() instanceof Literal lit) {
			index++;
			return lit.value();
		}
		throw error("expected a literal");
	}

	private boolean tryConsumeOperator(char symbol) {
		if (peekOperator(symbol)) {
			index++;
			return true;
		}
		return false;
	}

	private boolean peekOperator(char symbol) {
		return current() instanceof Operator op && op.symbol() == symbol;
	}

	private boolean peekChar(char c) {
		return current() instanceof Literal lit && lit.value() == c;
	}

	private char consumeChar() {
		Token token = current();
		if (token instanceof Operator op) {
			index++;
			return op.symbol();
		}
		if (token instanceof Literal lit) {
			index++;
			return lit.value();
		}
		throw error("expected an operator or a value");
	}

	private boolean tryConsumeChar(char c) {
		if (peekChar(c)) {
			index++;
			return true;
		}
		return false;
	}

	private void expect(char c) {
		if (!tryConsumeOperator(c)) {
			throw error("expected '" + c + "'");
		}
	}

	private Node parseAlternation() {
		Node left = parseConcatenation();

		while (tryConsumeOperator('|')) {
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
		return hasMore() && !peekOperator('|') && !peekOperator(')');
	}

	private Node parseRepetition() {
		Node node = parseAtom();

		while (hasMore()) {
			if (tryConsumeOperator('*')) node = new StarNode(node, tryConsumeOperator('?'));
			else if (tryConsumeOperator('+')) node = new PlusNode(node, tryConsumeOperator('?'));
			else if (tryConsumeOperator('?')) node = new OptionalNode(node, tryConsumeOperator('?'));
			else if (tryConsumeOperator('{')) node = parseBounds(node);
			else break;
		}
		return node;
	}

	private Node parseBounds(Node node) {
		int min = parseNumber();
		int max = min;
		boolean unbounded = false;

		if (tryConsumeChar(',')) {
			if (hasMore() && !peekOperator('}')) {
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
			if (min == 0) return new StarNode(node, false);
			if (min == 1) return new PlusNode(node, false);
			return new ConcatNode(required, new StarNode(node, false));
		}

		if (max == min) return required;
		if (min == 0 && max == 1) return new OptionalNode(node, false);

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
		Node result = new OptionalNode(node, false);
		for (int i = 1; i < count; i++) {
			result = new OptionalNode(new ConcatNode(node, result), false);
		}
		return result;
	}

	private int parseNumber() {
		int start = index;
		int number = 0;
		while (hasMore() && !peekChar(',') && !peekOperator('}')) {
			char c = consumeLiteral();
			if (!(c >= '0' && c <= '9')) throw error("expected a digit");
			number = number * 10 + (c - '0');
		}
		if (index == start) throw error("expected a digit");
		return number;
	}

	private Node parseAtom() {
		if (!hasMore()) {
			throw error("unexpected end of pattern");
		}

		if (tryConsumeOperator('^')) {
			return new StartAnchorNode();
		}

		if (tryConsumeOperator('$')) {
			return new EndAnchorNode();
		}

		if (tryConsumeOperator('(')) {
			if (tryConsumeOperator('?')) {
				if (!tryConsumeChar(':')) {
					throw error("unsupported group modifier");
				}
				Node inner = parseAlternation();
				expect(')');
				return inner;
			}
			int index = groupCount++;
			Node inner = parseAlternation();
			expect(')');
			return new GroupNode(inner, index);
		}

		if (tryConsumeOperator('[')) {
			Node charClass = buildCharSetNode();
			expect(']');
			return charClass;
		}

		if (tryConsumeOperator('.')) {
			return new CharSetNode(CharSet.all());
		}

		if (current() instanceof ClassShorthand cs) {
			index++;
			return new CharSetNode(CharSet.fromShorthand(cs.kind()));
		}

		if (current() instanceof Operator op) {
			throw error("unexpected '" + op.symbol() + "'");
		}

		char c = consumeLiteral();
		return new CharSetNode(CharSet.of(c));
	}

	private Node buildCharSetNode() {
		CharSet set = CharSet.empty();
		if (tryConsumeOperator('^')) set.negate();

		while (hasMoreCharClassItems()) {
			if (current() instanceof ClassShorthand cs) {
				index++;
				set.union(CharSet.fromShorthand(cs.kind()));
				continue;
			}

			char a = consumeChar();
			boolean isRangeChar = tryConsumeChar('-');

			if (isRangeChar && hasMoreCharClassItems()) {
				char b = consumeChar();
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
		return hasMore() && !peekOperator(']');
	}
}
