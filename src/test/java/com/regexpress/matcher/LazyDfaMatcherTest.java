package com.regexpress.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.regexpress.ast.nodes.Node;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

class LazyDfaMatcherTest {

	private static Matcher matcher(String pattern) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		return new LazyDfaMatcher(machine);
	}

	@Nested
	@DisplayName("a cached matcher answers the same as a fresh one")
	class CachedMatcherAnswersSameAsFresh {
		Matcher reuse = matcher("ab*");

		@Test
		@DisplayName("a reused matcher still matches a first input")
		void matchesFirstInput() {
			assertTrue(reuse.matches("ab"));
		}

		@Test
		@DisplayName("a reused matcher matches a longer input")
		void matchesLongerInput() {
			assertTrue(reuse.matches("abbb"));
		}

		@Test
		@DisplayName("a reused matcher rejects a mismatching input")
		void rejectsMismatchingInput() {
			assertFalse(reuse.matches("b"));
		}
	}

	@Nested
	@DisplayName("anchors, which depend on where the run is")
	class AnchorsDependOnPosition {
		@Test
		@DisplayName("a start anchor matches at the beginning")
		void startAnchorMatchesAtBeginning() {
			assertTrue(matcher("^a").matches("a"));
		}

		@Test
		@DisplayName("a start anchor does not match later")
		void startAnchorDoesNotMatchLater() {
			assertFalse(matcher("^a").matches("ba"));
		}

		@Test
		@DisplayName("an end anchor matches at the end")
		void endAnchorMatchesAtEnd() {
			assertTrue(matcher("a$").matches("a"));
		}

		@Test
		@DisplayName("an end anchor does not match mid-input")
		void endAnchorDoesNotMatchMidInput() {
			assertFalse(matcher("a$").matches("ab"));
		}

		@Test
		@DisplayName("both anchors match the whole input only")
		void bothAnchorsMatchWholeInputOnly() {
			assertTrue(matcher("^a$").matches("a"));
		}
	}

	@Nested
	@DisplayName("empty input and the minimum length")
	class EmptyInputAndMinimumLength {
		@Test
		@DisplayName("a nullable pattern matches the empty input")
		void nullablePatternMatchesEmptyInput() {
			assertTrue(matcher("a*").matches(""));
		}

		@Test
		@DisplayName("a plain pattern does not match the empty input")
		void plainPatternDoesNotMatchEmptyInput() {
			assertFalse(matcher("a").matches(""));
		}

		@Test
		@DisplayName("a minimum length rejects a too-short input")
		void minimumLengthRejectsTooShortInput() {
			assertFalse(matcher("a{3}").matches("aa"));
		}
	}

	@Test
	@DisplayName("a run that dies reports no match")
	void deadRunReportsNoMatch() {
		assertFalse(matcher("ab").matches("ax"));
	}

	@Nested
	@DisplayName("a concatenation of two loops across a shared alphabet")
	class ConcatenationOfTwoLoops {
		@Test
		@DisplayName("two loops full-match ordered input")
		void twoLoopsFullMatchOrderedInput() {
			assertTrue(matcher("(a|b)*(a|c)*").matches("aabcc"));
		}

		@Test
		@DisplayName("two loops reject shuffled input")
		void twoLoopsRejectShuffledInput() {
			assertFalse(matcher("(a|b)*(a|c)*").matches("acb"));
		}
	}

	// agreement with java.util.regex across every pattern and input combination, for the supported syntax
	static final String[] AGREEMENT_PATTERNS = { "a", "ab", "a|b", "a*", "ab*", "(a|b)*", "a(b|c)*", "(ab)*c",
			"a+", "a?b", "[abc]", "[a-c]", "[^ab]", ".", "a.c", "a{3}", "a{2,4}", "a{2,}", "a{0,1}b", "(ab){2}",
			"\\d", "\\D", "\\w", "\\W", "a\\*b", "a\\.b", "[\\da-f]", "[^\\d]",
			"^a", "a$", "^a$", "$", "a$b", "$a", "^*", "a$?",
			"a*?", "a+?", "a??", "<.*?>", "(?:ab)+" };
	static final String[] AGREEMENT_INPUTS = { "", "a", "b", "c", "d", "ab", "ba", "aab", "abc", "aaa", "aaaa", "aaaaa", "abab",
			"5", "_", " ", "*", ".", "a*b", "a.b" };

	static Stream<Arguments> patternInputCombinations() {
		return Stream.of(AGREEMENT_PATTERNS).flatMap(pattern ->
				Stream.of(AGREEMENT_INPUTS).map(input -> Arguments.of(pattern, input)));
	}

	@ParameterizedTest(name = "java.util.regex agrees for pattern \"{0}\" against input \"{1}\"")
	@MethodSource("patternInputCombinations")
	void agreesWithJavaUtilRegex(String pattern, String input) {
		boolean expected = Pattern.matches(pattern, input);
		assertEquals(expected, matcher(pattern).matches(input));
	}
}
