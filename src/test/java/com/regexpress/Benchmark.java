package com.regexpress;

import com.regexpress.matcher.Match;
import com.regexpress.matcher.PikeMatcher;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

// hand-rolled benchmark: warm-up, best of N rounds; prints timings, always exits 0
public class Benchmark {

	private static final int WARMUP = 2;
	private static final int ROUNDS = 5;

	public static void main(String[] args) {
		String text = text(100_000);
		// this family matches empty at every position, so no prefilter can skip it: measure it on less input
		String dense = text(20_000);

		System.out.printf("%-16s %-18s %9s %9s %9s%n", "family", "pattern", "best ms", "ns/char", "matches");
		bench("literal prefix", "hello[0-9]+", text);
		bench("first char set", "[aeiou]+", text);
		bench("alternation", "cat|dog|bird", text);
		bench("ambiguity", "(a|b)*(a|c)*", dense);
		bench("anchored", "^abc$", text);
	}

	private static void bench(String family, String pattern, String input) {
		Nfa machine = NfaBuilder.build(Parser.parse(pattern));

		int matches = 0;
		for (int i = 0; i < WARMUP; i++) {
			matches = findAll(machine, input);
		}

		long best = Long.MAX_VALUE;
		for (int round = 0; round < ROUNDS; round++) {
			long t0 = System.nanoTime();
			matches = findAll(machine, input);
			best = Math.min(best, System.nanoTime() - t0);
		}

		System.out.printf("%-16s %-18s %9.2f %9.1f %,9d%n",
			family, pattern, best / 1_000_000.0, best / (double) input.length(), matches);
	}

	// counts every occurrence, the way the facade's find-all loop does
	private static int findAll(Nfa machine, String input) {
		int found = 0;
		int from = 0;
		while (from <= input.length()) {
			Match match = PikeMatcher.find(machine, input, from);
			if (match == null) {
				break;
			}
			found++;
			from = match.end() == match.start() ? match.end() + 1 : match.end();
		}
		return found;
	}

	// deterministic pseudo-random lowercase text with a few planted matches
	private static String text(int size) {
		StringBuilder sb = new StringBuilder(size);
		long seed = 42;
		for (int i = 0; i < size; i++) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			sb.append((char) ('a' + (int) ((seed >>> 33) % 26)));
		}
		for (int i = 0; i < 10; i++) {
			int at = (i + 1) * (size / 11);
			sb.replace(at, at + 6, i % 2 == 0 ? "hello5" : "hellox");
		}
		return sb.toString();
	}
}
