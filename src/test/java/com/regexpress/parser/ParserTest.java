package com.regexpress.parser;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.CharSetNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.EmptyNode;
import com.regexpress.ast.EndAnchorNode;
import com.regexpress.ast.Node;
import com.regexpress.ast.OptionalNode;
import com.regexpress.ast.PlusNode;
import com.regexpress.ast.StarNode;
import com.regexpress.ast.StartAnchorNode;
import com.regexpress.tokenizer.RegexSyntaxException;

public class ParserTest {
	public static void main(String[] args) {
		// "ab*"
		Node expectedStarGripsOneChar = new ConcatNode(new CharSetNode(CharSet.of('a')), new StarNode(new CharSetNode(CharSet.of('b'))));
		// "(ab)*"
		Node expectedStarGripsGroup = new StarNode(new ConcatNode(new CharSetNode(CharSet.of('a')), new CharSetNode(CharSet.of('b'))));
		// "a|(bc)*"
		Node expectedMixedPrecedence = new AlternateNode(new CharSetNode(CharSet.of('a')), new StarNode(new ConcatNode(new CharSetNode(CharSet.of('b')), new CharSetNode(CharSet.of('c')))));

		// check precedence
		check("star grips only the character before it", expectedStarGripsOneChar, Parser.parse("ab*"));
		check("brackets let star apply to the whole group", expectedStarGripsGroup, Parser.parse("(ab)*"));
		check("alternation, grouping and repetition parse with correct precedence", expectedMixedPrecedence, Parser.parse("a|(bc)*"));

		Node parsedA1 = Parser.parse("a|b(c|d)*");
		Node parsedA2 = Parser.parse("a|b(c|d)*");
		Node parsedDifferent = Parser.parse("a|b(cd|e)*");

		// check equality
		check("parsing the same pattern twice gives equal trees", parsedA1, parsedA2);
		check("parsing different patterns gives unequal trees", parsedA1, parsedDifferent, false);

		// check degenerate patterns
		check("an empty pattern parses to an empty node", new EmptyNode(), Parser.parse(""));
		check("an empty group parses to an empty node", new EmptyNode(), Parser.parse("()"));
		check("an alternation with an empty right branch parses correctly", new AlternateNode(new CharSetNode(CharSet.of('a')), new EmptyNode()), Parser.parse("a|"));
		check("an alternation with both branches empty parses correctly", new AlternateNode(new EmptyNode(), new EmptyNode()), Parser.parse("|"));

		// "a**"
		Node stackedStars = new StarNode(new StarNode(new CharSetNode(CharSet.of('a'))));
		// "(a*)*"
		Node nestedClosure = new StarNode(new StarNode(new CharSetNode(CharSet.of('a'))));

		// check deliberate decisions
		check("stacked stars are accepted", stackedStars, Parser.parse("a**"));
		check("a star wrapped around a star is accepted without hanging", nestedClosure, Parser.parse("(a*)*"));

		Node charA = new CharSetNode(CharSet.of('a'));
		Node charB = new CharSetNode(CharSet.of('b'));

		// "a{3}" and "a{3,3}"
		Node exactCopies = new ConcatNode(new ConcatNode(charA, charA), charA);
		// "a{2,4}"
		Node boundedRange = new ConcatNode(new ConcatNode(charA, charA), new OptionalNode(new ConcatNode(charA, new OptionalNode(charA))));
		// "a{2,}"
		Node atLeastTwo = new ConcatNode(new ConcatNode(charA, charA), new StarNode(charA));
		// "(ab){2}"
		Node groupCopies = new ConcatNode(new ConcatNode(charA, charB), new ConcatNode(charA, charB));
		// "a{2}{2}"
		Node stackedBounds = new ConcatNode(new ConcatNode(charA, charA), new ConcatNode(charA, charA));

		// check bounded repetitions
		check("an exact bound expands to that many copies", exactCopies, Parser.parse("a{3}"));
		check("matching bounds expand to that many copies", exactCopies, Parser.parse("a{3,3}"));
		check("a bounded range expands to required copies followed by optional ones", boundedRange, Parser.parse("a{2,4}"));
		check("an open upper bound expands to required copies followed by a star", atLeastTwo, Parser.parse("a{2,}"));
		check("a bound after a group repeats the group", groupCopies, Parser.parse("(ab){2}"));
		check("stacked bounds are accepted", stackedBounds, Parser.parse("a{2}{2}"));

		// check normalized bounds
		check("an open bound with a zero minimum is a star", new StarNode(charA), Parser.parse("a{0,}"));
		check("an open bound with a one minimum is a plus", new PlusNode(charA), Parser.parse("a{1,}"));
		check("a zero-to-one bound is an optional", new OptionalNode(charA), Parser.parse("a{0,1}"));
		check("a zero bound parses to an empty node", new EmptyNode(), Parser.parse("a{0}"));

		String strayStar = "a|*b";
		String unopenedParen = "ab)c";
		String unclosedParen = "a(bc";
		String invertedRange = "a{2,1}";
		String missingMinimum = "a{,3}";
		String emptyBound = "a{}";
		String unterminatedBound = "a{2,";
		String emptyMaximum = "a{2,,}";

		// check rejections
		checkThrows("rejects a '*' with nothing to repeat", RegexSyntaxException.class, () -> Parser.parse(strayStar));
		checkThrows("rejects an unmatched closing parenthesis", RegexSyntaxException.class, () -> Parser.parse(unopenedParen));
		checkThrows("rejects an unclosed opening parenthesis", RegexSyntaxException.class, () -> Parser.parse(unclosedParen));
		checkThrows("rejects a maximum smaller than the minimum", RegexSyntaxException.class, () -> Parser.parse(invertedRange));
		checkThrows("rejects a missing minimum in a bound", RegexSyntaxException.class, () -> Parser.parse(missingMinimum));
		checkThrows("rejects an empty bound", RegexSyntaxException.class, () -> Parser.parse(emptyBound));
		checkThrows("rejects an unterminated bound", RegexSyntaxException.class, () -> Parser.parse(unterminatedBound));
		checkThrows("rejects an empty maximum in a bound", RegexSyntaxException.class, () -> Parser.parse(emptyMaximum));
		checkThrows("rejects a stray '{' with nothing to repeat", RegexSyntaxException.class, () -> Parser.parse("{3}"));
		checkThrows("rejects a stray '}' with no opening bound", RegexSyntaxException.class, () -> Parser.parse("a}"));

		// "[abc]"
		CharSet abcSet = CharSet.of('a');
		abcSet.union(CharSet.of('b'));
		abcSet.union(CharSet.of('c'));
		// "[a-]"
		CharSet aOrDashSet = CharSet.of('a');
		aOrDashSet.union(CharSet.of('-'));

		// check character classes
		check("a character class unions its members", new CharSetNode(abcSet), Parser.parse("[abc]"));
		check("a character class range parses to a CharSet range", new CharSetNode(CharSet.range('a', 'c')), Parser.parse("[a-c]"));
		check("a negated character class parses to a negated CharSet", new CharSetNode(CharSet.of('a').negate()), Parser.parse("[^a]"));
		check("a trailing dash in a character class is a literal dash", new CharSetNode(aOrDashSet), Parser.parse("[a-]"));

		// check escaping
		check("an escaped operator parses to that literal character", new CharSetNode(CharSet.of('*')), Parser.parse("\\*"));
		check("an escaped dot parses to a literal dot, not any-character", new CharSetNode(CharSet.of('.')), Parser.parse("\\."));
		check("an escaped opening parenthesis does not start a group", new ConcatNode(new ConcatNode(charA, new CharSetNode(CharSet.of('('))), charB), Parser.parse("a\\(b"));

		// "\d" and "\D"
		Node digitShorthand = new CharSetNode(CharSet.digit());
		Node notDigitShorthand = new CharSetNode(CharSet.digit().negate());
		// "[\da-f]"
		CharSet digitOrHexLetterSet = CharSet.digit();
		digitOrHexLetterSet.union(CharSet.range('a', 'f'));

		// check shorthand classes
		check("an escaped letter without shorthand meaning parses to a literal letter", new CharSetNode(CharSet.of('n')), Parser.parse("\\n"));
		check("\\d parses to the digit shorthand", digitShorthand, Parser.parse("\\d"));
		check("\\D parses to the negated digit shorthand", notDigitShorthand, Parser.parse("\\D"));
		check("a shorthand inside a character class unions with the rest of it", new CharSetNode(digitOrHexLetterSet), Parser.parse("[\\da-f]"));

		// check anchors
		check("a start anchor parses to a start anchor node", new StartAnchorNode(), Parser.parse("^"));
		check("an end anchor parses to an end anchor node", new EndAnchorNode(), Parser.parse("$"));
		check("a start anchor concatenates with what follows it", new ConcatNode(new StartAnchorNode(), charA), Parser.parse("^a"));
		check("an end anchor concatenates with what precedes it", new ConcatNode(charA, new EndAnchorNode()), Parser.parse("a$"));
		check("an anchor in the middle of a pattern still parses", new ConcatNode(new ConcatNode(charA, new StartAnchorNode()), charB), Parser.parse("a^b"));
		check("an escaped dollar parses to a literal dollar", new CharSetNode(CharSet.of('$')), Parser.parse("\\$"));

		report();
	}
}
