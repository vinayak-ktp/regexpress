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

		// check the newer operators
		check("plus matches a single repetition", true, matches("a+", "a"));
		check("plus matches several repetitions", true, matches("a+", "aaa"));
		check("plus does not match zero repetitions", false, matches("a+", ""));
		check("optional matches with its character", true, matches("ab?", "ab"));
		check("optional matches without its character", true, matches("ab?", "a"));
		check("optional does not match its character twice", false, matches("ab?", "abb"));
		check("a character class matches one of its characters", true, matches("[abc]", "b"));
		check("a character class rejects a character outside it", false, matches("[abc]", "d"));
		check("a ranged character class matches inside the range", true, matches("[a-c]", "b"));
		check("a ranged character class rejects outside the range", false, matches("[a-c]", "d"));
		check("a negated character class matches outside itself", true, matches("[^a]", "b"));
		check("a negated character class rejects inside itself", false, matches("[^a]", "a"));
		check("an exact bound matches that many characters", true, matches("a{3}", "aaa"));
		check("an exact bound rejects fewer characters", false, matches("a{3}", "aa"));
		check("an exact bound rejects more characters", false, matches("a{3}", "aaaa"));
		check("a bounded range matches inside the range", true, matches("a{2,4}", "aaa"));
		check("a bounded range rejects below its minimum", false, matches("a{2,4}", "a"));
		check("a bounded range rejects above its maximum", false, matches("a{2,4}", "aaaaa"));
		check("an open bound matches any amount above its minimum", true, matches("a{2,}", "aaaa"));
		check("an open bound rejects below its minimum", false, matches("a{2,}", "a"));

		// check full-match semantics
		check("an input longer than the pattern does not match", false, matches("abc", "abcd"));
		check("an input shorter than the pattern does not match", false, matches("abc", "ab"));

		// check degenerate patterns
		check("an empty pattern matches an empty input", true, matches("", ""));
		check("an empty pattern does not match a non-empty input", false, matches("", "a"));
		check("an alternation with an empty branch matches an empty input", true, matches("a|", ""));

		// check escaping
		check("an escaped operator matches only its literal character", true, matches("a\\*b", "a*b"));
		check("an escaped operator does not act as that operator", false, matches("a\\*b", "aaab"));
		check("an escaped dot matches only a literal dot", true, matches("a\\.b", "a.b"));
		check("an escaped dot does not match any character", false, matches("a\\.b", "axb"));

		// check anchors
		check("a start anchor matches at the start of the input", true, matches("^a", "a"));
		check("an end anchor matches at the end of the input", true, matches("a$", "a"));
		check("both anchors together match the input between them", true, matches("^a$", "a"));
		check("an end anchor alone matches the empty input", true, matches("$", ""));
		check("an end anchor alone rejects a non-empty input", false, matches("$", "a"));
		check("a lone start anchor rejects a non-empty input", false, matches("^", "a"));
		check("a start anchor in the middle of the pattern rejects everything", false, matches("a^b", "ab"));
		check("an end anchor in the middle of the pattern rejects everything", false, matches("$a", "a"));
		check("an escaped dollar matches a literal dollar", true, matches("\\$", "$"));

		// check shorthand classes
		check("\\d matches a digit", true, matches("\\d", "5"));
		check("\\d rejects a letter", false, matches("\\d", "a"));
		check("\\D matches a non-digit", true, matches("\\D", "a"));
		check("\\D rejects a digit", false, matches("\\D", "5"));
		check("\\w matches a letter, digit, or underscore", true, matches("\\w+", "abc_123"));
		check("\\w rejects a space", false, matches("\\w", " "));
		check("\\W matches a space", true, matches("\\W", " "));
		check("\\s matches a space", true, matches("\\s", " "));
		check("\\s matches a tab", true, matches("\\s", "\t"));
		check("\\s rejects a letter", false, matches("\\s", "a"));

		// check deliberate decisions
		check("the any character matches a newline", true, matches("a.c", "a\nc"));
		check("\\s does not include the vertical tab", false, matches("\\s", ""));

		// check shorthands combined with character classes
		check("a shorthand inside a class unions with the rest of the class", true, matches("[\\da-f]", "c"));
		check("a shorthand inside a class still rejects characters outside every member", false, matches("[\\da-f]", "g"));
		check("a negated shorthand inside a class contributes its complement", true, matches("[\\D]", "a"));
		check("negating a class around a negated shorthand cancels back to the positive set", true, matches("[^\\D]", "5"));
		check("negating a class around a negated shorthand still rejects what the shorthand rejected", false, matches("[^\\D]", "a"));
		check("two negated shorthands unioned together cover a digit", true, matches("[\\D\\S]", "5"));
		check("two negated shorthands unioned together cover a space", true, matches("[\\D\\S]", " "));

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

		// check deliberate decisions, kept out of the java.util.regex agreement suite
		check("stacked quantifiers match without hanging", true, matches("a**", "aaa"));
		check("stacked quantifiers match zero repetitions", true, matches("a**", ""));
		check("stacked bounds match the combined count", true, matches("a{2}{2}", "aaaa"));
		check("stacked bounds reject the uncombined count", false, matches("a{2}{2}", "aa"));

		// check agreement with java.util.regex across every pattern and input combination,
		// for the syntax this engine currently supports (literals, concatenation, alternation, star,
		// plus, optional, character classes, the any character, bounded repetition, escapes,
		// shorthand classes and anchors)
		String[] patterns = { "a", "ab", "a|b", "a*", "ab*", "(a|b)*", "a(b|c)*", "(ab)*c",
				"a+", "a?b", "[abc]", "[a-c]", "[^ab]", ".", "a.c", "a{3}", "a{2,4}", "a{2,}", "a{0,1}b", "(ab){2}",
				"\\d", "\\D", "\\w", "\\W", "a\\*b", "a\\.b", "[\\da-f]", "[^\\d]",
				"^a", "a$", "^a$", "$", "a$b", "$a", "^*", "a$?" };
		String[] inputs = { "", "a", "b", "c", "d", "ab", "ba", "aab", "abc", "aaa", "aaaa", "aaaaa", "abab",
				"5", "_", " ", "*", ".", "a*b", "a.b" };

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
