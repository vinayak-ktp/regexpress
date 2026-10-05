package com.regexpress.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.regexpress.Regex;
import com.regexpress.ast.nodes.Node;
import com.regexpress.matcher.Match;
import com.regexpress.matcher.PikeMatcher;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

class AstOptimiserTest {

	private static Node parse(String pattern) {
		return Parser.parse(pattern);
	}

	private static Node optimise(String pattern) {
		return AstOptimiser.optimise(parse(pattern));
	}

	private static String find(String pattern, String input) {
		com.regexpress.Match match = Regex.compile(pattern).find(input);
		return match == null ? "null" : match.start() + ".." + match.end();
	}

	private static String rawFind(String pattern, String input) {
		Match match = PikeMatcher.find(NfaBuilder.build(Parser.parse(pattern)), input);
		return match == null ? "null" : match.start() + ".." + match.end();
	}

	private static String javaFind(String pattern, String input) {
		java.util.regex.Matcher match = Pattern.compile(pattern).matcher(input);
		return match.find() ? match.start() + ".." + match.end() : "null";
	}

	@Nested
	@DisplayName("stacked quantifiers collapse")
	class StackedQuantifiersCollapse {
		@Test
		@DisplayName("a stacked star collapses")
		void stackedStarCollapses() {
			assertEquals(parse("a*"), optimise("a**"));
		}

		@Test
		@DisplayName("a plus over a star collapses")
		void plusOverStarCollapses() {
			assertEquals(parse("a*"), optimise("a*+"));
		}

		@Test
		@DisplayName("a star over a plus collapses")
		void starOverPlusCollapses() {
			assertEquals(parse("a*"), optimise("a+*"));
		}

		@Test
		@DisplayName("a star over an optional collapses")
		void starOverOptionalCollapses() {
			assertEquals(parse("a*"), optimise("a?*"));
		}

		@Test
		@DisplayName("an optional over an optional collapses")
		void optionalOverOptionalCollapses() {
			assertEquals(parse("a?"), optimise("(?:a?)?"));
		}

		@Test
		@DisplayName("a non-capturing group does not block collapsing")
		void nonCapturingGroupDoesNotBlockCollapsing() {
			assertEquals(parse("a*"), optimise("(?:a*)*"));
		}
	}

	@Nested
	@DisplayName("rewrites that would change preference are left alone")
	class PreferenceIsLeftAlone {
		@Test
		@DisplayName("a lazy quantifier is left alone")
		void lazyQuantifierLeftAlone() {
			assertEquals(parse("a+?"), optimise("a+?"));
		}

		@Test
		@DisplayName("a group blocks collapsing")
		void groupBlocksCollapsing() {
			assertEquals(parse("(a*)*"), optimise("(a*)*"));
		}
	}

	@Nested
	@DisplayName("alternations collapse and merge")
	class AlternationsCollapseAndMerge {
		@Test
		@DisplayName("equal alternatives collapse")
		void equalAlternativesCollapse() {
			assertEquals(parse("a"), optimise("a|a"));
		}

		@Test
		@DisplayName("unequal groups do not collapse")
		void unequalGroupsDoNotCollapse() {
			assertEquals(parse("(a)|(a)"), optimise("(a)|(a)"));
		}

		@Test
		@DisplayName("single characters merge into a class")
		void singleCharactersMergeIntoClass() {
			assertEquals(parse("[ab]"), optimise("a|b"));
		}

		@Test
		@DisplayName("a chain of single characters merges")
		void chainOfSingleCharactersMerges() {
			assertEquals(parse("[abc]"), optimise("a|b|c"));
		}

		@Test
		@DisplayName("classes merge with characters")
		void classesMergeWithCharacters() {
			assertEquals(parse("[abc]"), optimise("[ab]|c"));
		}
	}

	@Nested
	@DisplayName("empty drops out")
	class EmptyDropsOut {
		@Test
		@DisplayName("a quantifier over empty collapses")
		void quantifierOverEmptyCollapses() {
			assertEquals(parse(""), optimise("(?:)*"));
		}

		@Test
		@DisplayName("an empty prefix drops off")
		void emptyPrefixDropsOff() {
			assertEquals(parse("ab"), optimise("(?:)ab"));
		}
	}

	@Nested
	@DisplayName("the optimised facade still behaves like the unoptimised engine")
	class OptimisedFacadeBehavesLikeUnoptimisedEngine {
		@Test
		@DisplayName("a stacked star finds the same span")
		void stackedStarFindsSameSpan() {
			assertEquals(rawFind("a**", "aaa"), find("a**", "aaa"));
		}

		@Test
		@DisplayName("a stacked star on empty input finds the same span")
		void stackedStarOnEmptyInputFindsSameSpan() {
			assertEquals(rawFind("a**", ""), find("a**", ""));
		}

		@Test
		@DisplayName("a stacked plus-star finds the same span")
		void stackedPlusStarFindsSameSpan() {
			assertEquals(rawFind("a*+", "aaa"), find("a*+", "aaa"));
		}

		@Test
		@DisplayName("a collapsed star over a group finds the same span")
		void collapsedStarOverGroupFindsSameSpan() {
			assertEquals(rawFind("(?:a*)*", "aaa"), find("(?:a*)*", "aaa"));
		}

		@Test
		@DisplayName("a merged class finds like java")
		void mergedClassFindsLikeJava() {
			assertEquals(javaFind("a|b|c", "b"), find("a|b|c", "b"));
		}

		@Test
		@DisplayName("an empty prefix still matches")
		void emptyPrefixStillMatches() {
			assertTrue(Regex.matches("(?:)ab", "ab"));
		}
	}
}
