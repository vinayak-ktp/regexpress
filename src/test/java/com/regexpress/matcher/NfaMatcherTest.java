package com.regexpress.matcher;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.report;

import java.util.regex.Pattern;

import com.regexpress.ast.Node;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

public class NfaMatcherTest {
	public static void main(String[] args) {
		// check individual operators
		check("a single literal character matches itself", true, matches("a", "a"));
		check("alternation matches one of its branches", true, matches("a|b", "a"));
		check("concatenation matches characters in sequence", true, matches("ab", "ab"));
		check("star matches a single repetition", true, matches("a*", "a"));
		check("star matches zero repetitions", true, matches("a*", ""));
		check("star matches several repetitions", true, matches("a*", "aaa"));
		check("alternation does not match a character in neither branch", false, matches("a|b", "c"));
		check("star does not match a run with an extra wrong character", false, matches("a*", "aab"));

		// check full-match semantics
		check("an input longer than the pattern does not match", false, matches("abc", "abcd"));
		check("an input shorter than the pattern does not match", false, matches("abc", "ab"));

		// check degenerate patterns
		check("an empty pattern matches an empty input", true, matches("", ""));
		check("an empty pattern does not match a non-empty input", false, matches("", "a"));
		check("an alternation with an empty branch matches an empty input", true, matches("a|", ""));

		// check patterns that could hang the engine
		check("a star wrapped around a star matches without hanging", true, matches("(a*)*", "aaaaaaaaa"));
		check("ambiguous alternation inside a star rejects a long input quickly", false, matches("(a|a)*b", "a".repeat(30)));

		// check that the closure is taken before the first character is read
		check("a pattern starting with a group matches correctly", true, matches("(ab)c", "abc"));

		// check performance at scale
		check("a repeated group matches correctly over a very large input", true, matches("(ab)*", "ab".repeat(10000)));

		// check that precedence, already proven correct at the parser level, also holds once matched
		check("star grips only the character before it, allowing zero repetitions", true, matches("ab*", "a"));
		check("star grips only the character before it, allowing several repetitions", true, matches("ab*", "abb"));
		check("brackets change precedence, so a lone character does not match a whole-group star", false, matches("(ab)*", "a"));
		check("brackets change precedence, so repeated whole groups do match", true, matches("(ab)*", "abab"));

		// check agreement with java.util.regex across every pattern and input combination,
		// for the syntax this engine currently supports (literals, concatenation, alternation, star)
		String[] patterns = { "a", "ab", "a|b", "a*", "ab*", "(a|b)*", "a(b|c)*", "(ab)*c" };
		String[] inputs = { "", "a", "b", "ab", "ba", "aab", "abc", "abcabc", "aaaa" };

		for (String pattern : patterns) {
			for (String input : inputs) {
				boolean expected = Pattern.matches(pattern, input);
				check("java.util.regex agrees for pattern \"" + pattern + "\" against input \"" + input + "\"",
						expected, matches(pattern, input));
			}
		}

		report();
	}

	private static boolean matches(String pattern, String input) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		return NfaMatcher.matches(machine, input);
	}
}
