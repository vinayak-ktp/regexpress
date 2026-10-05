package com.regexpress.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.regexpress.ast.nodes.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.nodes.CharSetNode;
import com.regexpress.ast.nodes.ConcatNode;
import com.regexpress.ast.nodes.EmptyNode;
import com.regexpress.ast.nodes.EndAnchorNode;
import com.regexpress.ast.nodes.GroupNode;
import com.regexpress.ast.nodes.Node;
import com.regexpress.ast.nodes.OptionalNode;
import com.regexpress.ast.nodes.PlusNode;
import com.regexpress.ast.nodes.StarNode;
import com.regexpress.ast.nodes.StartAnchorNode;
import com.regexpress.tokenizer.RegexSyntaxException;

class ParserTest {

	Node charA = new CharSetNode(CharSet.of('a'));
	Node charB = new CharSetNode(CharSet.of('b'));

	@Nested
	@DisplayName("precedence")
	class Precedence {
		@Test
		@DisplayName("star grips only the character before it")
		void starGripsOneChar() {
			// "ab*"
			Node expected = new ConcatNode(charA, new StarNode(charB, false));
			assertEquals(expected, Parser.parse("ab*"));
		}

		@Test
		@DisplayName("brackets let star apply to the whole group")
		void bracketsLetStarApplyToGroup() {
			// "(ab)*"
			Node expected = new StarNode(new GroupNode(new ConcatNode(charA, charB), 0), false);
			assertEquals(expected, Parser.parse("(ab)*"));
		}

		@Test
		@DisplayName("alternation, grouping and repetition parse with correct precedence")
		void mixedPrecedence() {
			// "a|(bc)*"
			Node expected = new AlternateNode(charA, new StarNode(new GroupNode(new ConcatNode(charB, new CharSetNode(CharSet.of('c'))), 0), false));
			assertEquals(expected, Parser.parse("a|(bc)*"));
		}
	}

	@Nested
	@DisplayName("equality")
	class Equality {
		@Test
		@DisplayName("parsing the same pattern twice gives equal trees")
		void sameParsedTwiceIsEqual() {
			assertEquals(Parser.parse("a|b(c|d)*"), Parser.parse("a|b(c|d)*"));
		}

		@Test
		@DisplayName("parsing different patterns gives unequal trees")
		void differentPatternsAreUnequal() {
			assertNotEquals(Parser.parse("a|b(c|d)*"), Parser.parse("a|b(cd|e)*"));
		}
	}

	@Nested
	@DisplayName("degenerate patterns")
	class DegeneratePatterns {
		@Test
		@DisplayName("an empty pattern parses to an empty node")
		void emptyPatternIsEmptyNode() {
			assertEquals(new EmptyNode(), Parser.parse(""));
		}

		@Test
		@DisplayName("an empty group parses to a group around an empty node")
		void emptyGroupWrapsEmptyNode() {
			assertEquals(new GroupNode(new EmptyNode(), 0), Parser.parse("()"));
		}

		@Test
		@DisplayName("an alternation with an empty right branch parses correctly")
		void alternationWithEmptyRightBranch() {
			assertEquals(new AlternateNode(charA, new EmptyNode()), Parser.parse("a|"));
		}

		@Test
		@DisplayName("an alternation with both branches empty parses correctly")
		void alternationWithBothBranchesEmpty() {
			assertEquals(new AlternateNode(new EmptyNode(), new EmptyNode()), Parser.parse("|"));
		}
	}

	@Nested
	@DisplayName("deliberate decisions")
	class DeliberateDecisions {
		@Test
		@DisplayName("stacked stars are accepted")
		void stackedStarsAccepted() {
			// "a**"
			Node expected = new StarNode(new StarNode(charA, false), false);
			assertEquals(expected, Parser.parse("a**"));
		}

		@Test
		@DisplayName("a star wrapped around a star is accepted without hanging")
		void starWrappedAroundStar() {
			// "(a*)*"
			Node expected = new StarNode(new GroupNode(new StarNode(charA, false), 0), false);
			assertEquals(expected, Parser.parse("(a*)*"));
		}
	}

	@Nested
	@DisplayName("bounded repetitions")
	class BoundedRepetitions {
		@Test
		@DisplayName("an exact bound expands to that many copies")
		void exactBoundExpands() {
			// "a{3}"
			Node expected = new ConcatNode(new ConcatNode(charA, charA), charA);
			assertEquals(expected, Parser.parse("a{3}"));
		}

		@Test
		@DisplayName("matching bounds expand to that many copies")
		void matchingBoundsExpand() {
			// "a{3,3}"
			Node expected = new ConcatNode(new ConcatNode(charA, charA), charA);
			assertEquals(expected, Parser.parse("a{3,3}"));
		}

		@Test
		@DisplayName("a bounded range expands to required copies followed by optional ones")
		void boundedRangeExpands() {
			// "a{2,4}"
			Node expected = new ConcatNode(new ConcatNode(charA, charA), new OptionalNode(new ConcatNode(charA, new OptionalNode(charA, false)), false));
			assertEquals(expected, Parser.parse("a{2,4}"));
		}

		@Test
		@DisplayName("an open upper bound expands to required copies followed by a star")
		void openUpperBoundExpands() {
			// "a{2,}"
			Node expected = new ConcatNode(new ConcatNode(charA, charA), new StarNode(charA, false));
			assertEquals(expected, Parser.parse("a{2,}"));
		}

		@Test
		@DisplayName("a bound after a group repeats the group")
		void boundAfterGroupRepeatsGroup() {
			// "(ab){2}"
			Node expected = new ConcatNode(new GroupNode(new ConcatNode(charA, charB), 0), new GroupNode(new ConcatNode(charA, charB), 0));
			assertEquals(expected, Parser.parse("(ab){2}"));
		}

		@Test
		@DisplayName("stacked bounds are accepted")
		void stackedBoundsAccepted() {
			// "a{2}{2}"
			Node expected = new ConcatNode(new ConcatNode(charA, charA), new ConcatNode(charA, charA));
			assertEquals(expected, Parser.parse("a{2}{2}"));
		}
	}

	@Nested
	@DisplayName("normalized bounds")
	class NormalizedBounds {
		@Test
		@DisplayName("an open bound with a zero minimum is a star")
		void zeroMinimumIsStar() {
			assertEquals(new StarNode(charA, false), Parser.parse("a{0,}"));
		}

		@Test
		@DisplayName("an open bound with a one minimum is a plus")
		void oneMinimumIsPlus() {
			assertEquals(new PlusNode(charA, false), Parser.parse("a{1,}"));
		}

		@Test
		@DisplayName("a zero-to-one bound is an optional")
		void zeroToOneIsOptional() {
			assertEquals(new OptionalNode(charA, false), Parser.parse("a{0,1}"));
		}

		@Test
		@DisplayName("a zero bound parses to an empty node")
		void zeroBoundIsEmptyNode() {
			assertEquals(new EmptyNode(), Parser.parse("a{0}"));
		}
	}

	@Nested
	@DisplayName("lazy quantifiers")
	class LazyQuantifiers {
		@Test
		@DisplayName("a lazy star parses with the lazy flag")
		void lazyStarHasLazyFlag() {
			assertEquals(new StarNode(charA, true), Parser.parse("a*?"));
		}

		@Test
		@DisplayName("a lazy plus parses with the lazy flag")
		void lazyPlusHasLazyFlag() {
			assertEquals(new PlusNode(charA, true), Parser.parse("a+?"));
		}

		@Test
		@DisplayName("a lazy optional parses with the lazy flag")
		void lazyOptionalHasLazyFlag() {
			assertEquals(new OptionalNode(charA, true), Parser.parse("a??"));
		}

		@Test
		@DisplayName("a lazy quantifier does not equal its greedy twin")
		void lazyIsNotEqualToGreedy() {
			assertNotEquals(new StarNode(charA, true), Parser.parse("a*"));
		}

		@Test
		@DisplayName("a lazy star around a group still grips the group")
		void lazyStarGripsGroup() {
			assertEquals(new StarNode(new GroupNode(charA, 0), true), Parser.parse("(a)*?"));
		}
	}

	@Nested
	@DisplayName("non-capturing groups")
	class NonCapturingGroups {
		@Test
		@DisplayName("a non-capturing group leaves no node in the tree")
		void nonCapturingGroupLeavesNoNode() {
			assertEquals(new StarNode(new ConcatNode(charA, charB), false), Parser.parse("(?:ab)*"));
		}

		@Test
		@DisplayName("a non-capturing group does not consume a group number")
		void nonCapturingGroupDoesNotConsumeNumber() {
			Node expected = new ConcatNode(new ConcatNode(new GroupNode(charA, 0), charB), new GroupNode(new CharSetNode(CharSet.of('c')), 1));
			assertEquals(expected, Parser.parse("(a)(?:b)(c)"));
		}

		@Test
		@DisplayName("rejects an unsupported group modifier")
		void rejectsUnsupportedModifier() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("(?=a)"));
		}

		@Test
		@DisplayName("rejects a group opening with a bare question mark")
		void rejectsBareQuestionMark() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("(?a)"));
		}
	}

	@Nested
	@DisplayName("rejections")
	class Rejections {
		@Test
		@DisplayName("rejects a '*' with nothing to repeat")
		void rejectsStrayStar() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a|*b"));
		}

		@Test
		@DisplayName("rejects an unmatched closing parenthesis")
		void rejectsUnopenedParen() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("ab)c"));
		}

		@Test
		@DisplayName("rejects an unclosed opening parenthesis")
		void rejectsUnclosedParen() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a(bc"));
		}

		@Test
		@DisplayName("rejects a maximum smaller than the minimum")
		void rejectsInvertedRange() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a{2,1}"));
		}

		@Test
		@DisplayName("rejects a missing minimum in a bound")
		void rejectsMissingMinimum() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a{,3}"));
		}

		@Test
		@DisplayName("rejects an empty bound")
		void rejectsEmptyBound() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a{}"));
		}

		@Test
		@DisplayName("rejects an unterminated bound")
		void rejectsUnterminatedBound() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a{2,"));
		}

		@Test
		@DisplayName("rejects an empty maximum in a bound")
		void rejectsEmptyMaximum() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a{2,,}"));
		}

		@Test
		@DisplayName("rejects a stray '{' with nothing to repeat")
		void rejectsStrayOpenBrace() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("{3}"));
		}

		@Test
		@DisplayName("rejects a stray '}' with no opening bound")
		void rejectsStrayCloseBrace() {
			assertThrows(RegexSyntaxException.class, () -> Parser.parse("a}"));
		}
	}

	@Nested
	@DisplayName("character classes")
	class CharacterClasses {
		@Test
		@DisplayName("a character class unions its members")
		void classUnionsMembers() {
			// "[abc]"
			CharSet abcSet = CharSet.of('a');
			abcSet.union(CharSet.of('b'));
			abcSet.union(CharSet.of('c'));
			assertEquals(new CharSetNode(abcSet), Parser.parse("[abc]"));
		}

		@Test
		@DisplayName("a character class range parses to a CharSet range")
		void classRangeParsesToRange() {
			assertEquals(new CharSetNode(CharSet.range('a', 'c')), Parser.parse("[a-c]"));
		}

		@Test
		@DisplayName("a negated character class parses to a negated CharSet")
		void negatedClassParsesToNegatedSet() {
			assertEquals(new CharSetNode(CharSet.of('a').negate()), Parser.parse("[^a]"));
		}

		@Test
		@DisplayName("a trailing dash in a character class is a literal dash")
		void trailingDashIsLiteral() {
			// "[a-]"
			CharSet aOrDashSet = CharSet.of('a');
			aOrDashSet.union(CharSet.of('-'));
			assertEquals(new CharSetNode(aOrDashSet), Parser.parse("[a-]"));
		}
	}

	@Nested
	@DisplayName("escaping")
	class Escaping {
		@Test
		@DisplayName("an escaped operator parses to that literal character")
		void escapedOperatorIsLiteral() {
			assertEquals(new CharSetNode(CharSet.of('*')), Parser.parse("\\*"));
		}

		@Test
		@DisplayName("an escaped dot parses to a literal dot, not any-character")
		void escapedDotIsLiteral() {
			assertEquals(new CharSetNode(CharSet.of('.')), Parser.parse("\\."));
		}

		@Test
		@DisplayName("an escaped opening parenthesis does not start a group")
		void escapedParenDoesNotStartGroup() {
			Node expected = new ConcatNode(new ConcatNode(charA, new CharSetNode(CharSet.of('('))), charB);
			assertEquals(expected, Parser.parse("a\\(b"));
		}
	}

	@Nested
	@DisplayName("shorthand classes")
	class ShorthandClasses {
		@Test
		@DisplayName("an escaped letter without shorthand meaning parses to a literal letter")
		void escapedNonShorthandLetterIsLiteral() {
			assertEquals(new CharSetNode(CharSet.of('n')), Parser.parse("\\n"));
		}

		@Test
		@DisplayName("\\d parses to the digit shorthand")
		void dParsesToDigitShorthand() {
			assertEquals(new CharSetNode(CharSet.digit()), Parser.parse("\\d"));
		}

		@Test
		@DisplayName("\\D parses to the negated digit shorthand")
		void dCapsParsesToNegatedDigitShorthand() {
			assertEquals(new CharSetNode(CharSet.digit().negate()), Parser.parse("\\D"));
		}

		@Test
		@DisplayName("a shorthand inside a character class unions with the rest of it")
		void shorthandInsideClassUnions() {
			// "[\da-f]"
			CharSet digitOrHexLetterSet = CharSet.digit();
			digitOrHexLetterSet.union(CharSet.range('a', 'f'));
			assertEquals(new CharSetNode(digitOrHexLetterSet), Parser.parse("[\\da-f]"));
		}
	}

	@Nested
	@DisplayName("anchors")
	class Anchors {
		@Test
		@DisplayName("a start anchor parses to a start anchor node")
		void startAnchorParses() {
			assertEquals(new StartAnchorNode(), Parser.parse("^"));
		}

		@Test
		@DisplayName("an end anchor parses to an end anchor node")
		void endAnchorParses() {
			assertEquals(new EndAnchorNode(), Parser.parse("$"));
		}

		@Test
		@DisplayName("a start anchor concatenates with what follows it")
		void startAnchorConcatenatesWithFollowing() {
			assertEquals(new ConcatNode(new StartAnchorNode(), charA), Parser.parse("^a"));
		}

		@Test
		@DisplayName("an end anchor concatenates with what precedes it")
		void endAnchorConcatenatesWithPreceding() {
			assertEquals(new ConcatNode(charA, new EndAnchorNode()), Parser.parse("a$"));
		}

		@Test
		@DisplayName("an anchor in the middle of a pattern still parses")
		void anchorInMiddleStillParses() {
			assertEquals(new ConcatNode(new ConcatNode(charA, new StartAnchorNode()), charB), Parser.parse("a^b"));
		}

		@Test
		@DisplayName("an escaped dollar parses to a literal dollar")
		void escapedDollarIsLiteral() {
			assertEquals(new CharSetNode(CharSet.of('$')), Parser.parse("\\$"));
		}
	}
}
