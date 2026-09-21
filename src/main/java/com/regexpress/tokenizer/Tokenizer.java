package com.regexpress.tokenizer;

import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {

	private Tokenizer() { }

	public static List<Token> tokenize(String pattern) {
		List<Token> tokens = new ArrayList<>();
		boolean escaped = false;

		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);

			if (escaped) {
				tokens.add(isShorthand(c) ? new ClassShorthand(c, i) : new Literal(c, i));
				escaped = false;
			} else if (c == '\\') {
				escaped = true;
			} else {
				tokens.add(isOperator(c) ? new Operator(c, i) : new Literal(c, i));
			}
		}

		if (escaped) {
			throw new RegexSyntaxException("trailing backslash", pattern, pattern.length());
		}

		tokens.add(new End(pattern.length()));
		return tokens;
	}

	private static boolean isShorthand(char c) {
		String shorthands = "dDwWsS";
		return shorthands.indexOf(c) != -1;
	}

	private static boolean isOperator(char c) {
		String ops = "*+?|(){}[].^";
		return ops.indexOf(c) != -1;
	}
}
