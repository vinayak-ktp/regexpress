package com.regexpress.parser;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.CharSetNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.EmptyNode;
import com.regexpress.ast.Node;
import com.regexpress.ast.StarNode;

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

		String strayStar = "a|*b";
		String unopenedParen = "ab)c";
		String unclosedParen = "a(bc";

		// check rejections
		checkThrows("rejects a '*' with nothing to repeat", RegexSyntaxException.class, () -> Parser.parse(strayStar));
		checkThrows("rejects an unmatched closing parenthesis", RegexSyntaxException.class, () -> Parser.parse(unopenedParen));
		checkThrows("rejects an unclosed opening parenthesis", RegexSyntaxException.class, () -> Parser.parse(unclosedParen));

		report();
	}
}
