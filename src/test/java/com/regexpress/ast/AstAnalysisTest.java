package com.regexpress.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.regexpress.ast.nodes.Node;
import com.regexpress.parser.Parser;

class AstAnalysisTest {

	private static Node parse(String pattern) {
		return Parser.parse(pattern);
	}

	private static CharSet setOf(char... chars) {
		CharSet set = CharSet.empty();
		for (char c : chars) {
			set.union(CharSet.of(c));
		}
		return set;
	}

	@Nested
	@DisplayName("minimum lengths")
	class MinimumLengths {
		@Test
		@DisplayName("a literal spans one character")
		void literalSpansOne() {
			assertEquals(1, AstAnalysis.minLength(parse("a")));
		}

		@Test
		@DisplayName("a concatenation spans the sum")
		void concatenationSpansSum() {
			assertEquals(2, AstAnalysis.minLength(parse("ab")));
		}

		@Test
		@DisplayName("a star can match empty")
		void starCanMatchEmpty() {
			assertEquals(0, AstAnalysis.minLength(parse("a*")));
		}

		@Test
		@DisplayName("a plus spans its child")
		void plusSpansChild() {
			assertEquals(1, AstAnalysis.minLength(parse("a+")));
		}

		@Test
		@DisplayName("an alternation spans its shortest branch")
		void alternationSpansShortestBranch() {
			assertEquals(1, AstAnalysis.minLength(parse("a|bc")));
		}

		@Test
		@DisplayName("a bounded repetition spans its minimum")
		void boundedRepetitionSpansMinimum() {
			assertEquals(4, AstAnalysis.minLength(parse("(ab){2}")));
		}

		@Test
		@DisplayName("an open bound spans its minimum")
		void openBoundSpansMinimum() {
			assertEquals(2, AstAnalysis.minLength(parse("a{2,}")));
		}
	}

	@Nested
	@DisplayName("nullability")
	class Nullability {
		@Test
		@DisplayName("a plain pattern is not nullable")
		void plainPatternIsNotNullable() {
			assertFalse(AstAnalysis.nullable(parse("ab")));
		}

		@Test
		@DisplayName("a star is nullable")
		void starIsNullable() {
			assertTrue(AstAnalysis.nullable(parse("a*")));
		}

		@Test
		@DisplayName("an optional is nullable")
		void optionalIsNullable() {
			assertTrue(AstAnalysis.nullable(parse("a?")));
		}

		@Test
		@DisplayName("an empty pattern is nullable")
		void emptyPatternIsNullable() {
			assertTrue(AstAnalysis.nullable(parse("")));
		}
	}

	@Nested
	@DisplayName("first characters")
	class FirstCharacters {
		@Test
		@DisplayName("a concatenation starts with its head")
		void concatenationStartsWithHead() {
			assertEquals(setOf('a'), AstAnalysis.firstChars(parse("ab")));
		}

		@Test
		@DisplayName("an alternation starts with either branch")
		void alternationStartsWithEitherBranch() {
			assertEquals(setOf('a', 'b'), AstAnalysis.firstChars(parse("a|b")));
		}

		@Test
		@DisplayName("a nullable head passes the start on")
		void nullableHeadPassesStartOn() {
			assertEquals(setOf('a', 'b', 'c'), AstAnalysis.firstChars(parse("(a|b)*c")));
		}

		@Test
		@DisplayName("an optional head passes the start on")
		void optionalHeadPassesStartOn() {
			assertEquals(setOf('a', 'b'), AstAnalysis.firstChars(parse("a?b")));
		}

		@Test
		@DisplayName("a start anchor does not add start characters")
		void startAnchorAddsNoCharacters() {
			assertEquals(setOf('a'), AstAnalysis.firstChars(parse("^a")));
		}

		@Test
		@DisplayName("a class starts with its members")
		void classStartsWithMembers() {
			assertEquals(setOf('c', 'd'), AstAnalysis.firstChars(parse("[cd]")));
		}
	}

	@Nested
	@DisplayName("literal prefixes")
	class LiteralPrefixes {
		@Test
		@DisplayName("a plain literal is its own prefix")
		void plainLiteralIsOwnPrefix() {
			assertEquals("abc", AstAnalysis.literalPrefix(parse("abc")));
		}

		@Test
		@DisplayName("alternatives share their common prefix")
		void alternativesShareCommonPrefix() {
			assertEquals("ca", AstAnalysis.literalPrefix(parse("cat|car")));
		}

		@Test
		@DisplayName("alternatives without a common start have none")
		void alternativesWithoutCommonStartHaveNone() {
			assertEquals("", AstAnalysis.literalPrefix(parse("a|b")));
		}

		@Test
		@DisplayName("a prefix runs until something varies")
		void prefixRunsUntilSomethingVaries() {
			assertEquals("a", AstAnalysis.literalPrefix(parse("ab?c")));
		}

		@Test
		@DisplayName("a prefix crosses a group")
		void prefixCrossesGroup() {
			assertEquals("ab", AstAnalysis.literalPrefix(parse("(ab)")));
		}

		@Test
		@DisplayName("a prefix stops at a loop")
		void prefixStopsAtLoop() {
			assertEquals("", AstAnalysis.literalPrefix(parse("a*b")));
		}

		@Test
		@DisplayName("a prefix runs up to a class")
		void prefixRunsUpToClass() {
			assertEquals("hello", AstAnalysis.literalPrefix(parse("hello[0-9]+")));
		}

		@Test
		@DisplayName("equal alternatives keep the literal")
		void equalAlternativesKeepLiteral() {
			assertEquals("ab", AstAnalysis.literalPrefix(parse("ab|ab")));
		}
	}

	@Test
	@DisplayName("analysis does not mutate the tree's sets")
	void analysisDoesNotMutateTree() {
		Node tree = parse("a|b");
		AstAnalysis.firstChars(tree);
		AstAnalysis.literalPrefix(tree);
		assertEquals(parse("a|b"), tree);
	}
}
