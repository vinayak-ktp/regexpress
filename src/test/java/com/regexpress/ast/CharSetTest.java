package com.regexpress.ast;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

public class CharSetTest {
	public static void main(String[] args) {
		// check empty()
		check("an empty set contains no character", false, CharSet.empty().contains('a'));

		// check all()
		check("the all set contains an ordinary character", true, CharSet.all().contains('a'));
		check("the all set contains the null character", true, CharSet.all().contains('\0'));

		// check of(char)
		check("a single-character set contains that character", true, CharSet.of('a').contains('a'));
		check("a single-character set rejects a different character", false, CharSet.of('a').contains('b'));

		// check range(from, to)
		check("a range contains a character inside it", true, CharSet.range('a', 'c').contains('b'));
		check("a range contains its lower boundary", true, CharSet.range('a', 'c').contains('a'));
		check("a range contains its upper boundary", true, CharSet.range('a', 'c').contains('c'));
		check("a range rejects a character outside it", false, CharSet.range('a', 'c').contains('d'));

		// check negate()
		check("a negated set rejects the character it was built from", false, CharSet.of('a').negate().contains('a'));
		check("a negated set contains a character it was not built from", true, CharSet.of('a').negate().contains('b'));

		CharSet negatedTwice = CharSet.of('a').negate();
		negatedTwice.negate();

		// check deliberate decisions
		check("negating an already-negated set does not flip it back", false, negatedTwice.contains('a'));

		// check union(other)
		CharSet unioned = CharSet.of('a');
		unioned.union(CharSet.of('b'));
		check("a union contains a character from the first set", true, unioned.contains('a'));
		check("a union contains a character from the second set", true, unioned.contains('b'));
		check("a union rejects a character from neither set", false, unioned.contains('c'));

		// check union(other) with a negated operand
		CharSet setWithItsOwnComplement = CharSet.of('a');
		setWithItsOwnComplement.union(CharSet.of('a').negate());
		check("a set unioned with its own negation contains the character it started with", true, setWithItsOwnComplement.contains('a'));
		check("a set unioned with its own negation also contains every other character", true, setWithItsOwnComplement.contains('z'));

		// check digit()
		check("digit contains a digit", true, CharSet.digit().contains('5'));
		check("digit rejects a letter", false, CharSet.digit().contains('a'));

		// check word()
		check("word contains a letter", true, CharSet.word().contains('Z'));
		check("word contains a digit", true, CharSet.word().contains('5'));
		check("word contains an underscore", true, CharSet.word().contains('_'));
		check("word rejects a space", false, CharSet.word().contains(' '));

		// check whitespace()
		check("whitespace contains a space", true, CharSet.whitespace().contains(' '));
		check("whitespace contains a tab", true, CharSet.whitespace().contains('\t'));
		check("whitespace contains a newline", true, CharSet.whitespace().contains('\n'));
		check("whitespace rejects a letter", false, CharSet.whitespace().contains('a'));

		// check fromShorthand(kind)
		check("fromShorthand('d') agrees with digit()", true, CharSet.fromShorthand('d').contains('5'));
		check("fromShorthand('D') is the negation of digit()", true, CharSet.fromShorthand('D').contains('a'));
		check("fromShorthand('D') rejects a digit", false, CharSet.fromShorthand('D').contains('5'));
		check("fromShorthand('w') agrees with word()", true, CharSet.fromShorthand('w').contains('_'));
		check("fromShorthand('W') rejects a word character", false, CharSet.fromShorthand('W').contains('_'));
		check("fromShorthand('s') agrees with whitespace()", true, CharSet.fromShorthand('s').contains(' '));
		check("fromShorthand('S') rejects whitespace", false, CharSet.fromShorthand('S').contains(' '));

		// check rejections
		checkThrows("fromShorthand rejects a kind that is not one of dDwWsS", IllegalArgumentException.class, () -> CharSet.fromShorthand('x'));

		// check equals and toString
		check("two independently built ranges compare as equal", CharSet.range('a', 'z'), CharSet.range('a', 'z'));
		check("a set and its negation do not compare as equal", CharSet.of('a'), CharSet.of('a').negate(), false);
		check("a single character prints bare", "a", CharSet.of('a').toString());
		check("a range prints in brackets", "[a-z]", CharSet.range('a', 'z').toString());
		check("a negated range prints with a caret", "[^a-z]", CharSet.range('a', 'z').negate().toString());
		check("the all set prints as [all]", "[all]", CharSet.all().toString());

		report();
	}
}
