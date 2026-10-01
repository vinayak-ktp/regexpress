package com.regexpress.ast;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.report;

import com.regexpress.ast.nodes.Node;
import com.regexpress.parser.Parser;

public class AstAnalysisTest {
	public static void main(String[] args) {

		// check minimum lengths
		check("a literal spans one character", 1, AstAnalysis.minLength(parse("a")));
		check("a concatenation spans the sum", 2, AstAnalysis.minLength(parse("ab")));
		check("a star can match empty", 0, AstAnalysis.minLength(parse("a*")));
		check("a plus spans its child", 1, AstAnalysis.minLength(parse("a+")));
		check("an alternation spans its shortest branch", 1, AstAnalysis.minLength(parse("a|bc")));
		check("a bounded repetition spans its minimum", 4, AstAnalysis.minLength(parse("(ab){2}")));
		check("an open bound spans its minimum", 2, AstAnalysis.minLength(parse("a{2,}")));

		// check nullability
		check("a plain pattern is not nullable", false, AstAnalysis.nullable(parse("ab")));
		check("a star is nullable", true, AstAnalysis.nullable(parse("a*")));
		check("an optional is nullable", true, AstAnalysis.nullable(parse("a?")));
		check("an empty pattern is nullable", true, AstAnalysis.nullable(parse("")));

		// check first characters
		check("a concatenation starts with its head", setOf('a'), AstAnalysis.firstChars(parse("ab")));
		check("an alternation starts with either branch", setOf('a', 'b'), AstAnalysis.firstChars(parse("a|b")));
		check("a nullable head passes the start on", setOf('a', 'b', 'c'), AstAnalysis.firstChars(parse("(a|b)*c")));
		check("an optional head passes the start on", setOf('a', 'b'), AstAnalysis.firstChars(parse("a?b")));
		check("a start anchor does not add start characters", setOf('a'), AstAnalysis.firstChars(parse("^a")));
		check("a class starts with its members", setOf('c', 'd'), AstAnalysis.firstChars(parse("[cd]")));

		// check literal prefixes
		check("a plain literal is its own prefix", "abc", AstAnalysis.literalPrefix(parse("abc")));
		check("alternatives share their common prefix", "ca", AstAnalysis.literalPrefix(parse("cat|car")));
		check("alternatives without a common start have none", "", AstAnalysis.literalPrefix(parse("a|b")));
		check("a prefix runs until something varies", "a", AstAnalysis.literalPrefix(parse("ab?c")));
		check("a prefix crosses a group", "ab", AstAnalysis.literalPrefix(parse("(ab)")));
		check("a prefix stops at a loop", "", AstAnalysis.literalPrefix(parse("a*b")));
		check("a prefix runs up to a class", "hello", AstAnalysis.literalPrefix(parse("hello[0-9]+")));
		check("equal alternatives keep the literal", "ab", AstAnalysis.literalPrefix(parse("ab|ab")));

		// check the analysis leaves the tree untouched
		Node tree = parse("a|b");
		AstAnalysis.firstChars(tree);
		AstAnalysis.literalPrefix(tree);
		check("analysis does not mutate the tree's sets", parse("a|b"), tree);

		report();
	}

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
}
