package com.regexpress.matcher;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.report;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

public class PikeMatcherTest {
	public static void main(String[] args) {
		// check find returns the leftmost match
		check("a pattern that does not occur gives null", "null", describe(find("x", "abc")));
		check("find returns the leftmost match", "1..2 []", describe(find("b", "abcabc")));

		// check leftmost-first preference order
		check("the first listed alternative wins over a longer one", "0..4 [0, 1, 1, 4]",
			describe(find("(a|ab)(c|bcd)", "abcd")));
		check("the first listed alternative wins", "0..2 []", describe(find("ab|a", "ab")));

		// check greediness when preference does not interfere
		check("a greedy group takes as much as it can", "0..3 [0, 3, 3, 3]", describe(find("(a+)(a*)", "aaa")));

		// check lazy quantifiers prefer the shortest match
		check("a greedy any-star grabs as much as it can", "0..29 []", describe(find("<.*>", "<b>bold</b> and <i>italic</i>")));
		check("a lazy any-star stops at the first closing bracket", "0..3 []", describe(find("<.*?>", "<b>bold</b> and <i>italic</i>")));
		check("a lazy plus takes a single repetition", "0..1 []", describe(find("a+?", "aaa")));
		check("a lazy star prefers matching nothing", "0..0 []", describe(find("a*?", "aaa")));
		check("a lazy plus reports its single iteration", "0..1 [0, 1]", describe(find("(a|b)+?", "ab")));

		// check group captures
		check("a repeating group reports its last iteration", "0..2 [1, 2]", describe(find("(a|b)+", "ab")));
		check("an optional group that never runs stays -1", "0..1 [-1, -1]", describe(find("(a)?b", "b")));
		check("a group matching the empty string still records bounds", "0..1 [0, 0]", describe(find("(a*)b", "b")));
		check("nested groups are numbered by opening parenthesis", "0..3 [0, 3, 0, 1, 2, 3]",
			describe(find("((a)b(c))", "abc")));

		// check anchors during a search
		check("a start anchor does not match later in the input", "null", describe(find("^a", "ba")));
		check("an end anchor matches at the end of the input", "1..2 []", describe(find("a$", "ba")));

		// check empty matches
		check("a star finds an empty match when nothing else matches", "0..0 []", describe(find("a*", "b")));

		// check find from a position
		check("find resumes from the given position", "4..5 []", describe(find("b", "abcabc", 2)));
		check("find past every occurrence gives null", "null", describe(find("b", "abcabc", 5)));
		check("an empty match is found at the end too", "1..1 []", describe(find("a*", "b", 1)));

		// check all occurrences
		check("find-all lists every occurrence with fresh groups", "0..2 [0, 1, 1, 2]; 2..4 [2, 3, 3, 4]",
			findAll("(a)(b)", "abab"));

		// check performance at scale
		check("a repeated group matches a very large input", "0..20000 [19998, 20000]",
			describe(find("(ab)*", "ab".repeat(10000))));

		// check agreement with java.util.regex across every pattern and input combination,
		// for the syntax this engine currently supports (literals, concatenation, alternation,
		// star, plus, optional, character classes, the any character, bounded repetition,
		// escapes, anchors and capture groups) — every occurrence, with bounds and groups
		String[] patterns = {
			"a|ab", "ab|a", "a|bb", "a*", "a+", "a?", "b+", "ab*", "a*b", "a{2,3}",
			"(a|b)*", "a|b", "^a", "a$", "^a$", "^", "$", "x", "a{2,}",
			"(a|ab)(c|bcd)", "(a)(b)", "(a)?", "(a*)", "(a|b)+", "(ab)*c", "(a+)(a*)",
			"((a)b(c))", "(a(b(c)))", "(a|b|c)*", "(ab|a)(b?)",
			"<.*>", "<.*?>", "a+?", "a*?", "a*?b", "(a|b)+?"
		};
		String[] inputs = {
			"", "a", "b", "ab", "abc", "abcd", "abb", "abbbc", "aa", "aaa", "aab", "ba",
			"abbb", "xa", "xyz", "abab", "bbb", "cba", "xxa", "aab", "ababc", "abc"
		};

		for (String pattern : patterns) {
			Nfa machine = NfaBuilder.build(Parser.parse(pattern));
			for (String input : inputs) {
				check("java.util.regex agrees for pattern \"" + pattern + "\" against input \"" + input + "\"",
					findAllJava(pattern, input, machine.groupCount), findAll(pattern, input));
			}
		}

		report();
	}

	private static Match find(String pattern, String input) {
		return PikeMatcher.find(NfaBuilder.build(Parser.parse(pattern)), input);
	}

	private static Match find(String pattern, String input, int from) {
		return PikeMatcher.find(NfaBuilder.build(Parser.parse(pattern)), input, from);
	}

	private static String describe(Match match) {
		if (match == null) {
			return "null";
		}
		return match.start() + ".." + match.end() + " " + Arrays.toString(match.slots());
	}

	private static String findAll(String pattern, String input) {
		StringBuilder sb = new StringBuilder();
		int from = 0;
		while (from <= input.length()) {
			Match match = find(pattern, input, from);
			if (match == null) {
				break;
			}
			if (!sb.isEmpty()) {
				sb.append("; ");
			}
			sb.append(describe(match));
			from = match.end() == match.start() ? match.end() + 1 : match.end();
		}
		return sb.toString();
	}

	private static String findAllJava(String pattern, String input, int groupCount) {
		StringBuilder sb = new StringBuilder();
		Matcher matcher = Pattern.compile(pattern).matcher(input);
		while (matcher.find()) {
			if (!sb.isEmpty()) {
				sb.append("; ");
			}
			sb.append(describeJava(matcher, groupCount));
		}
		return sb.toString();
	}

	private static String describeJava(Matcher matcher, int groupCount) {
		StringBuilder sb = new StringBuilder(matcher.start() + ".." + matcher.end() + " [");
		for (int g = 1; g <= groupCount; g++) {
			if (g > 1) {
				sb.append(", ");
			}
			sb.append(matcher.start(g)).append(", ").append(matcher.end(g));
		}
		return sb.append("]").toString();
	}
}
