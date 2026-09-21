package com.regexpress.tokenizer;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

import java.util.List;

public class TokenizerTest {
	public static void main(String[] args) {
		// check ordinary characters and operators
		check("an ordinary character tokenizes as a literal", List.of(new Literal('a', 0), new End(1)), Tokenizer.tokenize("a"));
		check("a reserved character tokenizes as an operator", List.of(new Operator('*', 0), new End(1)), Tokenizer.tokenize("*"));
		check("positions advance across several tokens", List.of(new Literal('a', 0), new Operator('*', 1), new End(2)), Tokenizer.tokenize("a*"));

		// check escaping
		check("an escaped operator tokenizes as a literal", List.of(new Literal('*', 1), new End(2)), Tokenizer.tokenize("\\*"));
		check("an escaped backslash tokenizes as a literal backslash", List.of(new Literal('\\', 1), new End(2)), Tokenizer.tokenize("\\\\"));
		check("an escaped ordinary letter tokenizes as that literal letter", List.of(new Literal('n', 1), new End(2)), Tokenizer.tokenize("\\n"));

		// check class shorthands
		check("an escaped 'd' tokenizes as a class shorthand", List.of(new ClassShorthand('d', 1), new End(2)), Tokenizer.tokenize("\\d"));
		check("an escaped 'W' tokenizes as a class shorthand", List.of(new ClassShorthand('W', 1), new End(2)), Tokenizer.tokenize("\\W"));

		// check rejections
		checkThrows("a trailing backslash is rejected", RegexSyntaxException.class, () -> Tokenizer.tokenize("a\\"));

		report();
	}
}
