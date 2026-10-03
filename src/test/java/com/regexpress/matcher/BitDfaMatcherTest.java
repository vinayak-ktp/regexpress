package com.regexpress.matcher;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.report;

import java.util.regex.Pattern;

import com.regexpress.ast.nodes.Node;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

public class BitDfaMatcherTest {
	public static void main(String[] args) {

		// check a cached matcher answers the same as a fresh one
		Matcher reuse = matcher("ab*");
		check("a reused matcher still matches a first input", true, reuse.matches("ab"));
		check("a reused matcher matches a longer input", true, reuse.matches("abbb"));
		check("a reused matcher rejects a mismatching input", false, reuse.matches("b"));

		// check anchors, which depend on where the run is
		check("a start anchor matches at the beginning", true, matcher("^a").matches("a"));
		check("a start anchor does not match later", false, matcher("^a").matches("ba"));
		check("an end anchor matches at the end", true, matcher("a$").matches("a"));
		check("an end anchor does not match mid-input", false, matcher("a$").matches("ab"));
		check("both anchors match the whole input only", true, matcher("^a$").matches("a"));

		// check empty input and the minimum length
		check("a nullable pattern matches the empty input", true, matcher("a*").matches(""));
		check("a plain pattern does not match the empty input", false, matcher("a").matches(""));
		check("a minimum length rejects a too-short input", false, matcher("a{3}").matches("aa"));

		// check a dead run stops early
		check("a run that dies reports no match", false, matcher("ab").matches("ax"));

		// check a concatenation of two loops across a shared alphabet
		check("two loops full-match ordered input", true, matcher("(a|b)*(a|c)*").matches("aabcc"));
		check("two loops reject shuffled input", false, matcher("(a|b)*(a|c)*").matches("acb"));

		// check the factory routes by machine size
		check("a small machine uses the bit-parallel matcher", "BitDfaMatcher",
			matcher("ab*").getClass().getSimpleName());
		check("a large machine falls back to the set matcher", "LazyDfaMatcher",
			matcher("a".repeat(33)).getClass().getSimpleName());

		// check long literals through the bit path, close to the 64-state limit
		check("a long literal still matches through the bit path", true,
			matcher("a".repeat(20)).matches("a".repeat(20)));
		check("a long literal still rejects a shorter input", false,
			matcher("a".repeat(20)).matches("a".repeat(19)));

		// check agreement with java.util.regex across every pattern and input combination, for the supported syntax
		String[] patterns = { "a", "ab", "a|b", "a*", "ab*", "(a|b)*", "a(b|c)*", "(ab)*c",
				"a+", "a?b", "[abc]", "[a-c]", "[^ab]", ".", "a.c", "a{3}", "a{2,4}", "a{2,}", "a{0,1}b", "(ab){2}",
				"\\d", "\\D", "\\w", "\\W", "a\\*b", "a\\.b", "[\\da-f]", "[^\\d]",
				"^a", "a$", "^a$", "$", "a$b", "$a", "^*", "a$?",
				"a*?", "a+?", "a??", "<.*?>", "(?:ab)+" };
		String[] inputs = { "", "a", "b", "c", "d", "ab", "ba", "aab", "abc", "aaa", "aaaa", "aaaaa", "abab",
				"5", "_", " ", "*", ".", "a*b", "a.b" };

		for (String pattern : patterns) {
			Matcher machine = matcher(pattern);
			for (String input : inputs) {
				boolean expected = Pattern.matches(pattern, input);
				check("java.util.regex agrees for pattern \"" + pattern + "\" against input \"" + input + "\"",
					expected, machine.matches(input));
			}
		}

		report();
	}

	private static Matcher matcher(String pattern) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		return MatcherFactory.forMachine(machine);
	}
}
