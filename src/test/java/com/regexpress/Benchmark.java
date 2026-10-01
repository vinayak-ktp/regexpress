package com.regexpress;

import com.regexpress.matcher.Match;
import com.regexpress.matcher.PikeMatcher;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

// hand-rolled benchmark: warm-up, blackhole, best of N rounds; prints timings, always exits 0
public class Benchmark {

	private static final int WARMUP = 2;
	private static final int ROUNDS = 5;

	public static void main(String[] args) {
		String text = text(100_000);
		// this family matches empty at every position, so no prefilter can skip it: measure it on less input
		String dense = text(20_000);

		bench("literal prefix  hello[0-9]+", "hello[0-9]+", text);
		bench("first char set  [aeiou]+", "[aeiou]+", text);
		bench("alternation      cat|dog|bird", "cat|dog|bird", text);
		bench("ambiguity       (a|b)*(a|c)*", "(a|b)*(a|c)*", dense);
		bench("anchored        ^abc$", "^abc$", text);
	}

	private static void bench(String label, String pattern, String input) {
		System.out.println("running " + label);
		System.out.flush();
		Nfa machine = NfaBuilder.build(Parser.parse(pattern));

		long blackhole = 0;
		for (int i = 0; i < WARMUP; i++) {
			blackhole += findAll(machine, input);
		}

		long best = Long.MAX_VALUE;
		for (int round = 0; round < ROUNDS; round++) {
			long t0 = System.nanoTime();
			blackhole += findAll(machine, input);
			best = Math.min(best, System.nanoTime() - t0);
		}

		System.out.printf("%-32s %,12d ns   %,8.2f ns/char   checksum %d%n",
			label, best, best / (double) input.length(), blackhole % 1000);
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
