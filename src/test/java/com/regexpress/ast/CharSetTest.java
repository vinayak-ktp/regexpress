package com.regexpress.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CharSetTest {

	@Test
	@DisplayName("an empty set contains no character")
	void emptySetContainsNothing() {
		assertFalse(CharSet.empty().contains('a'));
	}

	@Nested
	@DisplayName("all()")
	class All {
		@Test
		@DisplayName("the all set contains an ordinary character")
		void containsOrdinary() {
			assertTrue(CharSet.all().contains('a'));
		}

		@Test
		@DisplayName("the all set contains the null character")
		void containsNullChar() {
			assertTrue(CharSet.all().contains('\0'));
		}
	}

	@Nested
	@DisplayName("of(char)")
	class Of {
		@Test
		@DisplayName("a single-character set contains that character")
		void containsItsChar() {
			assertTrue(CharSet.of('a').contains('a'));
		}

		@Test
		@DisplayName("a single-character set rejects a different character")
		void rejectsOtherChar() {
			assertFalse(CharSet.of('a').contains('b'));
		}
	}

	@Nested
	@DisplayName("range(from, to)")
	class Range {
		@Test
		@DisplayName("a range contains a character inside it")
		void containsInside() {
			assertTrue(CharSet.range('a', 'c').contains('b'));
		}

		@Test
		@DisplayName("a range contains its lower boundary")
		void containsLowerBound() {
			assertTrue(CharSet.range('a', 'c').contains('a'));
		}

		@Test
		@DisplayName("a range contains its upper boundary")
		void containsUpperBound() {
			assertTrue(CharSet.range('a', 'c').contains('c'));
		}

		@Test
		@DisplayName("a range rejects a character outside it")
		void rejectsOutside() {
			assertFalse(CharSet.range('a', 'c').contains('d'));
		}
	}

	@Nested
	@DisplayName("negate()")
	class Negate {
		@Test
		@DisplayName("a negated set rejects the character it was built from")
		void rejectsOwnChar() {
			assertFalse(CharSet.of('a').negate().contains('a'));
		}

		@Test
		@DisplayName("a negated set contains a character it was not built from")
		void containsOtherChar() {
			assertTrue(CharSet.of('a').negate().contains('b'));
		}

		@Test
		@DisplayName("negating an already-negated set does not flip it back")
		void doubleNegationDoesNotFlipBack() {
			CharSet negatedTwice = CharSet.of('a').negate();
			negatedTwice.negate();
			assertFalse(negatedTwice.contains('a'));
		}
	}

	@Nested
	@DisplayName("union(other)")
	class Union {
		@Test
		@DisplayName("a union contains a character from the first set")
		void containsFirstSetChar() {
			CharSet unioned = CharSet.of('a');
			unioned.union(CharSet.of('b'));
			assertTrue(unioned.contains('a'));
		}

		@Test
		@DisplayName("a union contains a character from the second set")
		void containsSecondSetChar() {
			CharSet unioned = CharSet.of('a');
			unioned.union(CharSet.of('b'));
			assertTrue(unioned.contains('b'));
		}

		@Test
		@DisplayName("a union rejects a character from neither set")
		void rejectsUnrelatedChar() {
			CharSet unioned = CharSet.of('a');
			unioned.union(CharSet.of('b'));
			assertFalse(unioned.contains('c'));
		}

		@Test
		@DisplayName("a set unioned with its own negation contains the character it started with")
		void unionWithOwnNegationContainsOriginal() {
			CharSet setWithItsOwnComplement = CharSet.of('a');
			setWithItsOwnComplement.union(CharSet.of('a').negate());
			assertTrue(setWithItsOwnComplement.contains('a'));
		}

		@Test
		@DisplayName("a set unioned with its own negation also contains every other character")
		void unionWithOwnNegationContainsEverythingElse() {
			CharSet setWithItsOwnComplement = CharSet.of('a');
			setWithItsOwnComplement.union(CharSet.of('a').negate());
			assertTrue(setWithItsOwnComplement.contains('z'));
		}
	}

	@Nested
	@DisplayName("digit()")
	class Digit {
		@Test
		@DisplayName("digit contains a digit")
		void containsDigit() {
			assertTrue(CharSet.digit().contains('5'));
		}

		@Test
		@DisplayName("digit rejects a letter")
		void rejectsLetter() {
			assertFalse(CharSet.digit().contains('a'));
		}
	}

	@Nested
	@DisplayName("word()")
	class Word {
		@Test
		@DisplayName("word contains a letter")
		void containsLetter() {
			assertTrue(CharSet.word().contains('Z'));
		}

		@Test
		@DisplayName("word contains a digit")
		void containsDigit() {
			assertTrue(CharSet.word().contains('5'));
		}

		@Test
		@DisplayName("word contains an underscore")
		void containsUnderscore() {
			assertTrue(CharSet.word().contains('_'));
		}

		@Test
		@DisplayName("word rejects a space")
		void rejectsSpace() {
			assertFalse(CharSet.word().contains(' '));
		}
	}

	@Nested
	@DisplayName("whitespace()")
	class Whitespace {
		@Test
		@DisplayName("whitespace contains a space")
		void containsSpace() {
			assertTrue(CharSet.whitespace().contains(' '));
		}

		@Test
		@DisplayName("whitespace contains a tab")
		void containsTab() {
			assertTrue(CharSet.whitespace().contains('\t'));
		}

		@Test
		@DisplayName("whitespace contains a newline")
		void containsNewline() {
			assertTrue(CharSet.whitespace().contains('\n'));
		}

		@Test
		@DisplayName("whitespace rejects a letter")
		void rejectsLetter() {
			assertFalse(CharSet.whitespace().contains('a'));
		}
	}

	@Nested
	@DisplayName("fromShorthand(kind)")
	class FromShorthand {
		@Test
		@DisplayName("fromShorthand('d') agrees with digit()")
		void dAgreesWithDigit() {
			assertTrue(CharSet.fromShorthand('d').contains('5'));
		}

		@Test
		@DisplayName("fromShorthand('D') is the negation of digit()")
		void dCapsIsNegationOfDigit() {
			assertTrue(CharSet.fromShorthand('D').contains('a'));
		}

		@Test
		@DisplayName("fromShorthand('D') rejects a digit")
		void dCapsRejectsDigit() {
			assertFalse(CharSet.fromShorthand('D').contains('5'));
		}

		@Test
		@DisplayName("fromShorthand('w') agrees with word()")
		void wAgreesWithWord() {
			assertTrue(CharSet.fromShorthand('w').contains('_'));
		}

		@Test
		@DisplayName("fromShorthand('W') rejects a word character")
		void wCapsRejectsWordChar() {
			assertFalse(CharSet.fromShorthand('W').contains('_'));
		}

		@Test
		@DisplayName("fromShorthand('s') agrees with whitespace()")
		void sAgreesWithWhitespace() {
			assertTrue(CharSet.fromShorthand('s').contains(' '));
		}

		@Test
		@DisplayName("fromShorthand('S') rejects whitespace")
		void sCapsRejectsWhitespace() {
			assertFalse(CharSet.fromShorthand('S').contains(' '));
		}

		@Test
		@DisplayName("fromShorthand rejects a kind that is not one of dDwWsS")
		void rejectsUnknownKind() {
			assertThrows(IllegalArgumentException.class, () -> CharSet.fromShorthand('x'));
		}
	}

	@Nested
	@DisplayName("equals and toString")
	class EqualsAndToString {
		@Test
		@DisplayName("two independently built ranges compare as equal")
		void equalRangesCompareEqual() {
			assertEquals(CharSet.range('a', 'z'), CharSet.range('a', 'z'));
		}

		@Test
		@DisplayName("a set and its negation do not compare as equal")
		void setAndNegationCompareUnequal() {
			assertNotEquals(CharSet.of('a'), CharSet.of('a').negate());
		}

		@Test
		@DisplayName("a single character prints bare")
		void singleCharPrintsBare() {
			assertEquals("a", CharSet.of('a').toString());
		}

		@Test
		@DisplayName("a range prints in brackets")
		void rangePrintsInBrackets() {
			assertEquals("[a-z]", CharSet.range('a', 'z').toString());
		}

		@Test
		@DisplayName("a negated range prints with a caret")
		void negatedRangePrintsWithCaret() {
			assertEquals("[^a-z]", CharSet.range('a', 'z').negate().toString());
		}

		@Test
		@DisplayName("the all set prints as [all]")
		void allSetPrintsAsAll() {
			assertEquals("[all]", CharSet.all().toString());
		}
	}
}
