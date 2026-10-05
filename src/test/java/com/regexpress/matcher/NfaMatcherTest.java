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

class NfaMatcherTest {

	private static boolean matches(String pattern, String input) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		return NfaMatcher.matches(machine, input);
	}

	@Nested
	@DisplayName("individual operators")
	class IndividualOperators {
		@Test
		@DisplayName("a single literal character matches itself")
		void literalMatchesItself() {
			assertTrue(matches("a", "a"));
		}

		@Test
		@DisplayName("alternation matches one of its branches")
		void alternationMatchesOneBranch() {
			assertTrue(matches("a|b", "a"));
		}

		@Test
		@DisplayName("concatenation matches characters in sequence")
		void concatenationMatchesInSequence() {
			assertTrue(matches("ab", "ab"));
		}

		@Test
		@DisplayName("star matches a single repetition")
		void starMatchesSingleRepetition() {
			assertTrue(matches("a*", "a"));
		}

		@Test
		@DisplayName("star matches zero repetitions")
		void starMatchesZeroRepetitions() {
			assertTrue(matches("a*", ""));
		}

		@Test
		@DisplayName("star matches several repetitions")
		void starMatchesSeveralRepetitions() {
			assertTrue(matches("a*", "aaa"));
		}

		@Test
		@DisplayName("alternation does not match a character in neither branch")
		void alternationRejectsCharInNeitherBranch() {
			assertFalse(matches("a|b", "c"));
		}

		@Test
		@DisplayName("star does not match a run with an extra wrong character")
		void starRejectsExtraWrongCharacter() {
			assertFalse(matches("a*", "aab"));
		}
	}

	@Nested
	@DisplayName("newer operators")
	class NewerOperators {
		@Test
		@DisplayName("plus matches a single repetition")
		void plusMatchesSingleRepetition() {
			assertTrue(matches("a+", "a"));
		}

		@Test
		@DisplayName("plus matches several repetitions")
		void plusMatchesSeveralRepetitions() {
			assertTrue(matches("a+", "aaa"));
		}

		@Test
		@DisplayName("plus does not match zero repetitions")
		void plusRejectsZeroRepetitions() {
			assertFalse(matches("a+", ""));
		}

		@Test
		@DisplayName("optional matches with its character")
		void optionalMatchesWithCharacter() {
			assertTrue(matches("ab?", "ab"));
		}

		@Test
		@DisplayName("optional matches without its character")
		void optionalMatchesWithoutCharacter() {
			assertTrue(matches("ab?", "a"));
		}

		@Test
		@DisplayName("optional does not match its character twice")
		void optionalRejectsCharacterTwice() {
			assertFalse(matches("ab?", "abb"));
		}

		@Test
		@DisplayName("a character class matches one of its characters")
		void classMatchesOneOfItsChars() {
			assertTrue(matches("[abc]", "b"));
		}

		@Test
		@DisplayName("a character class rejects a character outside it")
		void classRejectsCharOutside() {
			assertFalse(matches("[abc]", "d"));
		}

		@Test
		@DisplayName("a ranged character class matches inside the range")
		void rangedClassMatchesInside() {
			assertTrue(matches("[a-c]", "b"));
		}

		@Test
		@DisplayName("a ranged character class rejects outside the range")
		void rangedClassRejectsOutside() {
			assertFalse(matches("[a-c]", "d"));
		}

		@Test
		@DisplayName("a negated character class matches outside itself")
		void negatedClassMatchesOutside() {
			assertTrue(matches("[^a]", "b"));
		}

		@Test
		@DisplayName("a negated character class rejects inside itself")
		void negatedClassRejectsInside() {
			assertFalse(matches("[^a]", "a"));
		}

		@Test
		@DisplayName("an exact bound matches that many characters")
		void exactBoundMatchesThatManyChars() {
			assertTrue(matches("a{3}", "aaa"));
		}

		@Test
		@DisplayName("an exact bound rejects fewer characters")
		void exactBoundRejectsFewerChars() {
			assertFalse(matches("a{3}", "aa"));
		}

		@Test
		@DisplayName("an exact bound rejects more characters")
		void exactBoundRejectsMoreChars() {
			assertFalse(matches("a{3}", "aaaa"));
		}

		@Test
		@DisplayName("a bounded range matches inside the range")
		void boundedRangeMatchesInside() {
			assertTrue(matches("a{2,4}", "aaa"));
		}

		@Test
		@DisplayName("a bounded range rejects below its minimum")
		void boundedRangeRejectsBelowMinimum() {
			assertFalse(matches("a{2,4}", "a"));
		}

		@Test
		@DisplayName("a bounded range rejects above its maximum")
		void boundedRangeRejectsAboveMaximum() {
			assertFalse(matches("a{2,4}", "aaaaa"));
		}

		@Test
		@DisplayName("an open bound matches any amount above its minimum")
		void openBoundMatchesAboveMinimum() {
			assertTrue(matches("a{2,}", "aaaa"));
		}

		@Test
		@DisplayName("an open bound rejects below its minimum")
		void openBoundRejectsBelowMinimum() {
			assertFalse(matches("a{2,}", "a"));
		}
	}

	@Nested
	@DisplayName("lazy quantifiers keep the same language as their greedy twins")
	class LazyQuantifiersSameLanguage {
		@Test
		@DisplayName("a lazy star matches zero repetitions")
		void lazyStarMatchesZeroRepetitions() {
			assertTrue(matches("a*?", ""));
		}

		@Test
		@DisplayName("a lazy star matches several repetitions")
		void lazyStarMatchesSeveralRepetitions() {
			assertTrue(matches("a*?", "aaa"));
		}

		@Test
		@DisplayName("a lazy star rejects what its greedy twin rejects")
		void lazyStarRejectsWhatGreedyRejects() {
			assertFalse(matches("a*?", "aab"));
		}

		@Test
		@DisplayName("a lazy any-star full-matches like its greedy twin")
		void lazyAnyStarFullMatchesLikeGreedy() {
			assertTrue(matches("<.*?>", "<b>x</b>"));
		}

		@Test
		@DisplayName("a lazy any-star rejects like its greedy twin")
		void lazyAnyStarRejectsLikeGreedy() {
			assertFalse(matches("<.*?>", "a<b>"));
		}
	}

	@Nested
	@DisplayName("non-capturing groups")
	class NonCapturingGroups {
		@Test
		@DisplayName("a non-capturing group still groups")
		void nonCapturingGroupStillGroups() {
			assertTrue(matches("(?:ab)+", "abab"));
		}

		@Test
		@DisplayName("a non-capturing group rejects like a capturing one")
		void nonCapturingGroupRejectsLikeCapturing() {
			assertFalse(matches("(?:ab)+", "ababa"));
		}
	}

	@Nested
	@DisplayName("full-match semantics")
	class FullMatchSemantics {
		@Test
		@DisplayName("an input longer than the pattern does not match")
		void longerInputDoesNotMatch() {
			assertFalse(matches("abc", "abcd"));
		}

		@Test
		@DisplayName("an input shorter than the pattern does not match")
		void shorterInputDoesNotMatch() {
			assertFalse(matches("abc", "ab"));
		}
	}

	@Nested
	@DisplayName("degenerate patterns")
	class DegeneratePatterns {
		@Test
		@DisplayName("an empty pattern matches an empty input")
		void emptyPatternMatchesEmptyInput() {
			assertTrue(matches("", ""));
		}

		@Test
		@DisplayName("an empty pattern does not match a non-empty input")
		void emptyPatternRejectsNonEmptyInput() {
			assertFalse(matches("", "a"));
		}

		@Test
		@DisplayName("an alternation with an empty branch matches an empty input")
		void alternationWithEmptyBranchMatchesEmptyInput() {
			assertTrue(matches("a|", ""));
		}
	}

	@Nested
	@DisplayName("escaping")
	class Escaping {
		@Test
		@DisplayName("an escaped operator matches only its literal character")
		void escapedOperatorMatchesOnlyLiteral() {
			assertTrue(matches("a\\*b", "a*b"));
		}

		@Test
		@DisplayName("an escaped operator does not act as that operator")
		void escapedOperatorDoesNotAct() {
			assertFalse(matches("a\\*b", "aaab"));
		}

		@Test
		@DisplayName("an escaped dot matches only a literal dot")
		void escapedDotMatchesOnlyLiteral() {
			assertTrue(matches("a\\.b", "a.b"));
		}

		@Test
		@DisplayName("an escaped dot does not match any character")
		void escapedDotDoesNotMatchAnyChar() {
			assertFalse(matches("a\\.b", "axb"));
		}
	}

	@Nested
	@DisplayName("anchors")
	class Anchors {
		@Test
		@DisplayName("a start anchor matches at the start of the input")
		void startAnchorMatchesAtStart() {
			assertTrue(matches("^a", "a"));
		}

		@Test
		@DisplayName("an end anchor matches at the end of the input")
		void endAnchorMatchesAtEnd() {
			assertTrue(matches("a$", "a"));
		}

		@Test
		@DisplayName("both anchors together match the input between them")
		void bothAnchorsMatchBetweenThem() {
			assertTrue(matches("^a$", "a"));
		}

		@Test
		@DisplayName("an end anchor alone matches the empty input")
		void endAnchorAloneMatchesEmptyInput() {
			assertTrue(matches("$", ""));
		}

		@Test
		@DisplayName("an end anchor alone rejects a non-empty input")
		void endAnchorAloneRejectsNonEmptyInput() {
			assertFalse(matches("$", "a"));
		}

		@Test
		@DisplayName("a lone start anchor rejects a non-empty input")
		void loneStartAnchorRejectsNonEmptyInput() {
			assertFalse(matches("^", "a"));
		}

		@Test
		@DisplayName("a start anchor in the middle of the pattern rejects everything")
		void startAnchorInMiddleRejectsEverything() {
			assertFalse(matches("a^b", "ab"));
		}

		@Test
		@DisplayName("an end anchor in the middle of the pattern rejects everything")
		void endAnchorInMiddleRejectsEverything() {
			assertFalse(matches("$a", "a"));
		}

		@Test
		@DisplayName("an escaped dollar matches a literal dollar")
		void escapedDollarMatchesLiteralDollar() {
			assertTrue(matches("\\$", "$"));
		}
	}

	@Nested
	@DisplayName("shorthand classes")
	class ShorthandClasses {
		@Test
		@DisplayName("\\d matches a digit")
		void dMatchesDigit() {
			assertTrue(matches("\\d", "5"));
		}

		@Test
		@DisplayName("\\d rejects a letter")
		void dRejectsLetter() {
			assertFalse(matches("\\d", "a"));
		}

		@Test
		@DisplayName("\\D matches a non-digit")
		void dCapsMatchesNonDigit() {
			assertTrue(matches("\\D", "a"));
		}

		@Test
		@DisplayName("\\D rejects a digit")
		void dCapsRejectsDigit() {
			assertFalse(matches("\\D", "5"));
		}

		@Test
		@DisplayName("\\w matches a letter, digit, or underscore")
		void wMatchesLetterDigitOrUnderscore() {
			assertTrue(matches("\\w+", "abc_123"));
		}

		@Test
		@DisplayName("\\w rejects a space")
		void wRejectsSpace() {
			assertFalse(matches("\\w", " "));
		}

		@Test
		@DisplayName("\\W matches a space")
		void wCapsMatchesSpace() {
			assertTrue(matches("\\W", " "));
		}

		@Test
		@DisplayName("\\s matches a space")
		void sMatchesSpace() {
			assertTrue(matches("\\s", " "));
		}

		@Test
		@DisplayName("\\s matches a tab")
		void sMatchesTab() {
			assertTrue(matches("\\s", "\t"));
		}

		@Test
		@DisplayName("\\s rejects a letter")
		void sRejectsLetter() {
			assertFalse(matches("\\s", "a"));
		}
	}

	@Nested
	@DisplayName("deliberate decisions")
	class DeliberateDecisions {
		@Test
		@DisplayName("the any character matches a newline")
		void anyCharacterMatchesNewline() {
			assertTrue(matches("a.c", "a\nc"));
		}

		@Test
		@DisplayName("\\s does not include the vertical tab")
		void sExcludesVerticalTab() {
			assertFalse(matches("\\s", "\u000B"));
		}
	}

	@Nested
	@DisplayName("shorthands combined with character classes")
	class ShorthandsCombinedWithClasses {
		@Test
		@DisplayName("a shorthand inside a class unions with the rest of the class")
		void shorthandInsideClassUnions() {
			assertTrue(matches("[\\da-f]", "c"));
		}

		@Test
		@DisplayName("a shorthand inside a class still rejects characters outside every member")
		void shorthandInsideClassRejectsOutside() {
			assertFalse(matches("[\\da-f]", "g"));
		}

		@Test
		@DisplayName("a negated shorthand inside a class contributes its complement")
		void negatedShorthandInsideClassContributesComplement() {
			assertTrue(matches("[\\D]", "a"));
		}

		@Test
		@DisplayName("negating a class around a negated shorthand cancels back to the positive set")
		void negatingAroundNegatedShorthandCancelsBack() {
			assertTrue(matches("[^\\D]", "5"));
		}

		@Test
		@DisplayName("negating a class around a negated shorthand still rejects what the shorthand rejected")
		void negatingAroundNegatedShorthandStillRejects() {
			assertFalse(matches("[^\\D]", "a"));
		}

		@Test
		@DisplayName("two negated shorthands unioned together cover a digit")
		void twoNegatedShorthandsCoverDigit() {
			assertTrue(matches("[\\D\\S]", "5"));
		}

		@Test
		@DisplayName("two negated shorthands unioned together cover a space")
		void twoNegatedShorthandsCoverSpace() {
			assertTrue(matches("[\\D\\S]", " "));
		}
	}

	@Nested
	@DisplayName("patterns that could hang the engine")
	class PatternsThatCouldHang {
		@Test
		@DisplayName("a star wrapped around a star matches without hanging")
		void starWrappedAroundStarMatchesWithoutHanging() {
			assertTrue(matches("(a*)*", "aaaaaaaaa"));
		}

		@Test
		@DisplayName("ambiguous alternation inside a star rejects a long input quickly")
		void ambiguousAlternationInsideStarRejectsQuickly() {
			assertFalse(matches("(a|a)*b", "a".repeat(30)));
		}
	}

	@Test
	@DisplayName("a pattern starting with a group matches correctly")
	void patternStartingWithGroupMatches() {
		assertTrue(matches("(ab)c", "abc"));
	}

	@Test
	@DisplayName("a repeated group matches correctly over a very large input")
	void repeatedGroupMatchesOverLargeInput() {
		assertTrue(matches("(ab)*", "ab".repeat(10000)));
	}

	@Nested
	@DisplayName("precedence, already proven correct at the parser level, also holds once matched")
	class PrecedenceHoldsOnceMatched {
		@Test
		@DisplayName("star grips only the character before it, allowing zero repetitions")
		void starGripsOneCharAllowingZero() {
			assertTrue(matches("ab*", "a"));
		}

		@Test
		@DisplayName("star grips only the character before it, allowing several repetitions")
		void starGripsOneCharAllowingSeveral() {
			assertTrue(matches("ab*", "abb"));
		}

		@Test
		@DisplayName("brackets change precedence, so a lone character does not match a whole-group star")
		void bracketsChangePrecedenceRejectsLoneChar() {
			assertFalse(matches("(ab)*", "a"));
		}

		@Test
		@DisplayName("brackets change precedence, so repeated whole groups do match")
		void bracketsChangePrecedenceMatchesRepeatedGroups() {
			assertTrue(matches("(ab)*", "abab"));
		}
	}

	@Nested
	@DisplayName("deliberate decisions, kept out of the java.util.regex agreement suite")
	class DeliberateDecisionsOutOfAgreement {
		@Test
		@DisplayName("stacked quantifiers match without hanging")
		void stackedQuantifiersMatchWithoutHanging() {
			assertTrue(matches("a**", "aaa"));
		}

		@Test
		@DisplayName("stacked quantifiers match zero repetitions")
		void stackedQuantifiersMatchZeroRepetitions() {
			assertTrue(matches("a**", ""));
		}

		@Test
		@DisplayName("stacked bounds match the combined count")
		void stackedBoundsMatchCombinedCount() {
			assertTrue(matches("a{2}{2}", "aaaa"));
		}

		@Test
		@DisplayName("stacked bounds reject the uncombined count")
		void stackedBoundsRejectUncombinedCount() {
			assertFalse(matches("a{2}{2}", "aa"));
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
		assertEquals(expected, matches(pattern, input));
	}
}
