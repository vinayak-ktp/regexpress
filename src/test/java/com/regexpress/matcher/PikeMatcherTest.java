package com.regexpress.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

class PikeMatcherTest {

	private static Match find(String pattern, String input) {
		return PikeMatcher.find(NfaBuilder.build(Parser.parse(pattern)), input);
	}

	private static Match find(String pattern, String input, int from) {
		return PikeMatcher.find(NfaBuilder.build(Parser.parse(pattern)), input, from);
	}

	private static String replaceAll(String pattern, String input, String replacement) {
		return PikeMatcher.replaceAll(NfaBuilder.build(Parser.parse(pattern)), input, replacement);
	}

	private static List<String> split(String pattern, String input) {
		return PikeMatcher.split(NfaBuilder.build(Parser.parse(pattern)), input);
	}

	private static String describe(Match match) {
		if (match == null) {
			return "null";
		}
		return match.start() + ".." + match.end() + " " + Arrays.toString(match.slots());
	}

	private static String findAll(String pattern, String input) {
		StringBuilder sb = new StringBuilder();
		int from = 0;
		while (from <= input.length()) {
			Match match = find(pattern, input, from);
			if (match == null) {
				break;
			}
			if (!sb.isEmpty()) {
				sb.append("; ");
			}
			sb.append(describe(match));
			from = match.end() == match.start() ? match.end() + 1 : match.end();
		}
		return sb.toString();
	}

	private static String findAllJava(String pattern, String input, int groupCount) {
		StringBuilder sb = new StringBuilder();
		Matcher matcher = Pattern.compile(pattern).matcher(input);
		while (matcher.find()) {
			if (!sb.isEmpty()) {
				sb.append("; ");
			}
			sb.append(describeJava(matcher, groupCount));
		}
		return sb.toString();
	}

	private static String describeJava(Matcher matcher, int groupCount) {
		StringBuilder sb = new StringBuilder(matcher.start() + ".." + matcher.end() + " [");
		for (int g = 1; g <= groupCount; g++) {
			if (g > 1) {
				sb.append(", ");
			}
			sb.append(matcher.start(g)).append(", ").append(matcher.end(g));
		}
		return sb.append("]").toString();
	}

	@Nested
	@DisplayName("find returns the leftmost match")
	class FindReturnsLeftmostMatch {
		@Test
		@DisplayName("a pattern that does not occur gives null")
		void patternNotOccurringGivesNull() {
			assertEquals("null", describe(find("x", "abc")));
		}

		@Test
		@DisplayName("find returns the leftmost match")
		void findReturnsLeftmostMatch() {
			assertEquals("1..2 []", describe(find("b", "abcabc")));
		}
	}

	@Nested
	@DisplayName("leftmost-first preference order")
	class LeftmostFirstPreferenceOrder {
		@Test
		@DisplayName("the first listed alternative wins over a longer one")
		void firstListedAlternativeWinsOverLonger() {
			assertEquals("0..4 [0, 1, 1, 4]", describe(find("(a|ab)(c|bcd)", "abcd")));
		}

		@Test
		@DisplayName("the first listed alternative wins")
		void firstListedAlternativeWins() {
			assertEquals("0..2 []", describe(find("ab|a", "ab")));
		}
	}

	@Test
	@DisplayName("a greedy group takes as much as it can")
	void greedyGroupTakesAsMuchAsItCan() {
		assertEquals("0..3 [0, 3, 3, 3]", describe(find("(a+)(a*)", "aaa")));
	}

	@Nested
	@DisplayName("lazy quantifiers prefer the shortest match")
	class LazyQuantifiersPreferShortestMatch {
		@Test
		@DisplayName("a greedy any-star grabs as much as it can")
		void greedyAnyStarGrabsAsMuchAsItCan() {
			assertEquals("0..29 []", describe(find("<.*>", "<b>bold</b> and <i>italic</i>")));
		}

		@Test
		@DisplayName("a lazy any-star stops at the first closing bracket")
		void lazyAnyStarStopsAtFirstClosingBracket() {
			assertEquals("0..3 []", describe(find("<.*?>", "<b>bold</b> and <i>italic</i>")));
		}

		@Test
		@DisplayName("a lazy plus takes a single repetition")
		void lazyPlusTakesSingleRepetition() {
			assertEquals("0..1 []", describe(find("a+?", "aaa")));
		}

		@Test
		@DisplayName("a lazy star prefers matching nothing")
		void lazyStarPrefersMatchingNothing() {
			assertEquals("0..0 []", describe(find("a*?", "aaa")));
		}

		@Test
		@DisplayName("a lazy plus reports its single iteration")
		void lazyPlusReportsSingleIteration() {
			assertEquals("0..1 [0, 1]", describe(find("(a|b)+?", "ab")));
		}
	}

	@Test
	@DisplayName("a non-capturing group leaves no slot")
	void nonCapturingGroupLeavesNoSlot() {
		assertEquals("0..2 [1, 2]", describe(find("(?:a)(b)", "ab")));
	}

	@Nested
	@DisplayName("the prefilter skips positions without changing results")
	class PrefilterSkipsPositionsWithoutChangingResults {
		@Test
		@DisplayName("a literal prefix jumps past non-starters")
		void literalPrefixJumpsPastNonStarters() {
			assertEquals("6..9 [8, 9]", describe(find("ca(t|r)", "xxcaxxcat")));
		}

		@Test
		@DisplayName("a first-character scan jumps past non-starters")
		void firstCharacterScanJumpsPastNonStarters() {
			assertEquals("2..4 []", describe(find("[0-9]x", "ab3x")));
		}

		@Test
		@DisplayName("a nullable pattern still finds empty matches")
		void nullablePatternStillFindsEmptyMatches() {
			assertEquals("0..0 []", describe(find("a*", "bbb")));
		}

		@Test
		@DisplayName("a minimum length gives no match on short input")
		void minimumLengthGivesNoMatchOnShortInput() {
			assertEquals("null", describe(find("a{3}", "aa")));
		}

		@Test
		@DisplayName("a prefix search resumes from a position")
		void prefixSearchResumesFromPosition() {
			assertEquals("6..9 [8, 9]", describe(find("ca(t|r)", "catcarcat", 4)));
		}
	}

	@Nested
	@DisplayName("group captures")
	class GroupCaptures {
		@Test
		@DisplayName("a repeating group reports its last iteration")
		void repeatingGroupReportsLastIteration() {
			assertEquals("0..2 [1, 2]", describe(find("(a|b)+", "ab")));
		}

		@Test
		@DisplayName("an optional group that never runs stays -1")
		void optionalGroupThatNeverRunsStaysMinusOne() {
			assertEquals("0..1 [-1, -1]", describe(find("(a)?b", "b")));
		}

		@Test
		@DisplayName("a group matching the empty string still records bounds")
		void groupMatchingEmptyStringStillRecordsBounds() {
			assertEquals("0..1 [0, 0]", describe(find("(a*)b", "b")));
		}

		@Test
		@DisplayName("nested groups are numbered by opening parenthesis")
		void nestedGroupsNumberedByOpeningParenthesis() {
			assertEquals("0..3 [0, 3, 0, 1, 2, 3]", describe(find("((a)b(c))", "abc")));
		}
	}

	@Nested
	@DisplayName("anchors during a search")
	class AnchorsDuringSearch {
		@Test
		@DisplayName("a start anchor does not match later in the input")
		void startAnchorDoesNotMatchLater() {
			assertEquals("null", describe(find("^a", "ba")));
		}

		@Test
		@DisplayName("an end anchor matches at the end of the input")
		void endAnchorMatchesAtEnd() {
			assertEquals("1..2 []", describe(find("a$", "ba")));
		}
	}

	@Test
	@DisplayName("a star finds an empty match when nothing else matches")
	void starFindsEmptyMatchWhenNothingElseMatches() {
		assertEquals("0..0 []", describe(find("a*", "b")));
	}

	@Nested
	@DisplayName("find from a position")
	class FindFromPosition {
		@Test
		@DisplayName("find resumes from the given position")
		void findResumesFromGivenPosition() {
			assertEquals("4..5 []", describe(find("b", "abcabc", 2)));
		}

		@Test
		@DisplayName("find past every occurrence gives null")
		void findPastEveryOccurrenceGivesNull() {
			assertEquals("null", describe(find("b", "abcabc", 5)));
		}

		@Test
		@DisplayName("an empty match is found at the end too")
		void emptyMatchFoundAtEndToo() {
			assertEquals("1..1 []", describe(find("a*", "b", 1)));
		}
	}

	@Test
	@DisplayName("find-all lists every occurrence with fresh groups")
	void findAllListsEveryOccurrenceWithFreshGroups() {
		assertEquals("0..2 [0, 1, 1, 2]; 2..4 [2, 3, 3, 4]", findAll("(a)(b)", "abab"));
	}

	@Nested
	@DisplayName("replace built on find-all")
	class ReplaceBuiltOnFindAll {
		@Test
		@DisplayName("replace swaps every occurrence")
		void replaceSwapsEveryOccurrence() {
			assertEquals("the X and the X", replaceAll("cat|dog", "the cat and the dog", "X"));
		}

		@Test
		@DisplayName("replace keeps the text between matches")
		void replaceKeepsTextBetweenMatches() {
			assertEquals("XbX", replaceAll("a", "aba", "X"));
		}

		@Test
		@DisplayName("replace of a pattern matching empty fills the gaps")
		void replaceOfPatternMatchingEmptyFillsGaps() {
			assertEquals("-b-b-b-", replaceAll("a*", "bbb", "-"));
		}

		@Test
		@DisplayName("replace with no match returns the input")
		void replaceWithNoMatchReturnsInput() {
			assertEquals("abc", replaceAll("x", "abc", "X"));
		}
	}

	@Nested
	@DisplayName("split built on find-all")
	class SplitBuiltOnFindAll {
		@Test
		@DisplayName("split collects the gaps between matches")
		void splitCollectsGapsBetweenMatches() {
			assertEquals(List.of("a", "b", "c"), split("\\d+", "a1b22c"));
		}

		@Test
		@DisplayName("split keeps a leading empty piece but drops trailing empties")
		void splitKeepsLeadingEmptyDropsTrailingEmpties() {
			assertEquals(List.of("", "a"), split("\\d", "1a2"));
		}

		@Test
		@DisplayName("split with no match returns the whole input as one piece")
		void splitWithNoMatchReturnsWholeInput() {
			assertEquals(List.of("abc"), split("x", "abc"));
		}

		@Test
		@DisplayName("split drops every trailing empty piece")
		void splitDropsEveryTrailingEmptyPiece() {
			assertEquals(List.of(), split("a*", "aaa"));
		}

		@Test
		@DisplayName("split skips an empty leading match")
		void splitSkipsEmptyLeadingMatch() {
			assertEquals(List.of(","), split("x*", ","));
		}

		@Test
		@DisplayName("split on an empty pattern splits between characters")
		void splitOnEmptyPatternSplitsBetweenCharacters() {
			assertEquals(List.of("a", "b"), split("", "ab"));
		}
	}

	@Test
	@DisplayName("a repeated group matches a very large input")
	void repeatedGroupMatchesVeryLargeInput() {
		assertEquals("0..20000 [19998, 20000]", describe(find("(ab)*", "ab".repeat(10000))));
	}

	// agreement with java.util.regex across every pattern and input combination, for the supported syntax
	static final String[] AGREEMENT_PATTERNS = {
			"a|ab", "ab|a", "a|bb", "a*", "a+", "a?", "b+", "ab*", "a*b", "a{2,3}",
			"(a|b)*", "a|b", "^a", "a$", "^a$", "^", "$", "x", "a{2,}",
			"(a|ab)(c|bcd)", "(a)(b)", "(a)?", "(a*)", "(a|b)+", "(ab)*c", "(a+)(a*)",
			"((a)b(c))", "(a(b(c)))", "(a|b|c)*", "(ab|a)(b?)",
			"<.*>", "<.*?>", "a+?", "a*?", "a*?b", "(a|b)+?",
			"(?:a)(b)", "(?:ab)+", "(?:a|b)*c"
	};
	static final String[] AGREEMENT_INPUTS = {
			"", "a", "b", "ab", "abc", "abcd", "abb", "abbbc", "aa", "aaa", "aab", "ba",
			"abbb", "xa", "xyz", "abab", "bbb", "cba", "xxa", "aab", "ababc", "abc"
	};

	static Stream<Arguments> patternInputCombinations() {
		return Stream.of(AGREEMENT_PATTERNS).flatMap(pattern ->
				Stream.of(AGREEMENT_INPUTS).map(input -> Arguments.of(pattern, input)));
	}

	@ParameterizedTest(name = "java.util.regex agrees on find-all for pattern \"{0}\" against input \"{1}\"")
	@MethodSource("patternInputCombinations")
	void agreesWithJavaUtilRegexOnFindAll(String pattern, String input) {
		int groupCount = NfaBuilder.build(Parser.parse(pattern)).groupCount;
		assertEquals(findAllJava(pattern, input, groupCount), findAll(pattern, input));
	}

	@ParameterizedTest(name = "java.util.regex agrees on replaceAll for pattern \"{0}\" against input \"{1}\"")
	@MethodSource("patternInputCombinations")
	void agreesWithJavaUtilRegexOnReplaceAll(String pattern, String input) {
		assertEquals(input.replaceAll(pattern, "-"), replaceAll(pattern, input, "-"));
	}

	@ParameterizedTest(name = "java.util.regex agrees on split for pattern \"{0}\" against input \"{1}\"")
	@MethodSource("patternInputCombinations")
	void agreesWithJavaUtilRegexOnSplit(String pattern, String input) {
		assertEquals(List.of(input.split(pattern)), split(pattern, input));
	}
}
