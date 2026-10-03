package com.regexpress.ast;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.report;

import java.util.regex.Pattern;

import com.regexpress.Regex;
import com.regexpress.ast.nodes.Node;
import com.regexpress.matcher.Match;
import com.regexpress.matcher.PikeMatcher;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

public class AstOptimiserTest {
	public static void main(String[] args) {

		// check stacked quantifiers collapse
		check("a stacked star collapses", parse("a*"), optimise("a**"));
		check("a plus over a star collapses", parse("a*"), optimise("a*+"));
		check("a star over a plus collapses", parse("a*"), optimise("a+*"));
		check("a star over an optional collapses", parse("a*"), optimise("a?*"));
		check("an optional over an optional collapses", parse("a?"), optimise("(?:a?)?"));
		check("a non-capturing group does not block collapsing", parse("a*"), optimise("(?:a*)*"));

		// check rewrites that would change preference are left alone
		check("a lazy quantifier is left alone", parse("a+?"), optimise("a+?"));
		check("a group blocks collapsing", parse("(a*)*"), optimise("(a*)*"));

		// check alternations collapse and merge
		check("equal alternatives collapse", parse("a"), optimise("a|a"));
		check("unequal groups do not collapse", parse("(a)|(a)"), optimise("(a)|(a)"));
		check("single characters merge into a class", parse("[ab]"), optimise("a|b"));
		check("a chain of single characters merges", parse("[abc]"), optimise("a|b|c"));
		check("classes merge with characters", parse("[abc]"), optimise("[ab]|c"));

		// check empty drops out
		check("a quantifier over empty collapses", parse(""), optimise("(?:)*"));
		check("an empty prefix drops off", parse("ab"), optimise("(?:)ab"));

		// check the optimised facade still behaves like the unoptimised engine
		check("a stacked star finds the same span", rawFind("a**", "aaa"), find("a**", "aaa"));
		check("a stacked star on empty input finds the same span", rawFind("a**", ""), find("a**", ""));
		check("a stacked plus-star finds the same span", rawFind("a*+", "aaa"), find("a*+", "aaa"));
		check("a collapsed star over a group finds the same span", rawFind("(?:a*)*", "aaa"), find("(?:a*)*", "aaa"));
		check("a merged class finds like java", javaFind("a|b|c", "b"), find("a|b|c", "b"));
		check("an empty prefix still matches", true, Regex.matches("(?:)ab", "ab"));

		report();
	}

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
}
