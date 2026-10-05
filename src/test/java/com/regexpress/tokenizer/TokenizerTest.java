package com.regexpress.tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.regexpress.tokenizer.tokens.ClassShorthand;
import com.regexpress.tokenizer.tokens.End;
import com.regexpress.tokenizer.tokens.Literal;
import com.regexpress.tokenizer.tokens.Operator;

class TokenizerTest {

	@Nested
	@DisplayName("ordinary characters and operators")
	class OrdinaryCharactersAndOperators {

		@Test
		@DisplayName("an ordinary character tokenizes as a literal")
		void ordinaryCharIsLiteral() {
			assertEquals(List.of(new Literal('a', 0), new End(1)), Tokenizer.tokenize("a"));
		}

		@Test
		@DisplayName("a reserved character tokenizes as an operator")
		void reservedCharIsOperator() {
			assertEquals(List.of(new Operator('*', 0), new End(1)), Tokenizer.tokenize("*"));
		}

		@Test
		@DisplayName("a dollar tokenizes as an operator")
		void dollarIsOperator() {
			assertEquals(List.of(new Operator('$', 0), new End(1)), Tokenizer.tokenize("$"));
		}

		@Test
		@DisplayName("positions advance across several tokens")
		void positionsAdvance() {
			assertEquals(List.of(new Literal('a', 0), new Operator('*', 1), new End(2)), Tokenizer.tokenize("a*"));
		}
	}

	@Nested
	@DisplayName("escaping")
	class Escaping {

		@Test
		@DisplayName("an escaped operator tokenizes as a literal")
		void escapedOperatorIsLiteral() {
			assertEquals(List.of(new Literal('*', 1), new End(2)), Tokenizer.tokenize("\\*"));
		}

		@Test
		@DisplayName("an escaped backslash tokenizes as a literal backslash")
		void escapedBackslashIsLiteral() {
			assertEquals(List.of(new Literal('\\', 1), new End(2)), Tokenizer.tokenize("\\\\"));
		}

		@Test
		@DisplayName("an escaped ordinary letter tokenizes as that literal letter")
		void escapedOrdinaryLetterIsLiteral() {
			assertEquals(List.of(new Literal('n', 1), new End(2)), Tokenizer.tokenize("\\n"));
		}

		@Test
		@DisplayName("an escaped dollar tokenizes as a literal")
		void escapedDollarIsLiteral() {
			assertEquals(List.of(new Literal('$', 1), new End(2)), Tokenizer.tokenize("\\$"));
		}
	}

	@Nested
	@DisplayName("class shorthands")
	class ClassShorthands {

		@Test
		@DisplayName("an escaped 'd' tokenizes as a class shorthand")
		void escapedDIsShorthand() {
			assertEquals(List.of(new ClassShorthand('d', 1), new End(2)), Tokenizer.tokenize("\\d"));
		}

		@Test
		@DisplayName("an escaped 'W' tokenizes as a class shorthand")
		void escapedWCapsIsShorthand() {
			assertEquals(List.of(new ClassShorthand('W', 1), new End(2)), Tokenizer.tokenize("\\W"));
		}
	}

	@Test
	@DisplayName("a trailing backslash is rejected")
	void trailingBackslashIsRejected() {
		assertThrows(RegexSyntaxException.class, () -> Tokenizer.tokenize("a\\"));
	}
}
