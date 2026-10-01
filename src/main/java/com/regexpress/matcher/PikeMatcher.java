package com.regexpress.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.regexpress.nfa.Assertion;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.State;

public final class PikeMatcher {

	private PikeMatcher() { }

	private record Candidate(State state, int start, int[] slots) { }

	private static final class CandidateList {
		private final List<Candidate> candidates;
		private final Set<State> seen;
		private boolean sealed;
		private Candidate winner;

		public CandidateList() {
			candidates = new ArrayList<>();
			seen = new HashSet<>();
		}

		private boolean add(Candidate candidate) {
			if (sealed) {
				return false;
			}
			if (!seen.add(candidate.state())) {
				return false;
			}

			candidates.add(candidate);
			if (candidate.state().accepting()) {
				winner = candidate;
				sealed = true;
			}
			return true;
		}

		private List<Candidate> list() {
			return candidates;
		}

		private boolean hasAccepting() {
			return sealed;
		}

		private Candidate winner() {
			return winner;
		}
	}

	public static Match find(Nfa machine, String input) {
		return find(machine, input, 0);
	}

	// finds the leftmost match that starts at or after `from`
	public static Match find(Nfa machine, String input, int from) {
		int length = input.length();
		if (!machine.nullable) {
			// nothing is in flight yet: skip straight to the next position a match could start at
			from = nextStart(machine, input, from);
			if (from < 0) {
				return null;
			}
		}
		CandidateList current = new CandidateList();
		addCandidate(current, new Candidate(machine.start, from, freshSlots(machine)), from, length);

		Match match = null;

		for (int i = from; i <= length; i++) {
			if (match == null && i > from) {
				if (current.list().isEmpty() && !machine.nullable) {
					// no thread in flight and no match yet: jump to the next possible start
					int next = nextStart(machine, input, i);
					if (next < 0) {
						return null;
					}
					i = next;
				}
				addCandidate(current, new Candidate(machine.start, i, freshSlots(machine)), i, length);
			}

			if (current.hasAccepting()) {
				match = new Match(current.winner().start(), i, current.winner().slots());
			}

			// nothing left in flight: the match can neither grow nor be replaced
			if (match != null && current.list().isEmpty()) {
				break;
			}

			if (i == length) break;

			current = step(current, input.charAt(i), i + 1, length);
		}

		return match;
	}

	// the next position a match could start at, or -1 when there is none
	private static int nextStart(Nfa machine, String input, int from) {
		int last = input.length() - machine.minLength;
		if (from > last) {
			return -1;
		}
		if (!machine.literalPrefix.isEmpty()) {
			int at = input.indexOf(machine.literalPrefix, from);
			return at >= 0 && at <= last ? at : -1;
		}
		int j = from;
		while (j <= last && !machine.firstChars.contains(input.charAt(j))) {
			j++;
		}
		return j <= last ? j : -1;
	}

	// replaces every match with the given literal text
	public static String replaceAll(Nfa machine, String input, String replacement) {
		StringBuilder result = new StringBuilder();
		int last = 0;
		int from = 0;
		while (from <= input.length()) {
			Match match = find(machine, input, from);
			if (match == null) {
				break;
			}
			result.append(input, last, match.start());
			result.append(replacement);
			last = match.end();
			from = match.end() == match.start() ? match.end() + 1 : match.end();
		}
		result.append(input, last, input.length());
		return result.toString();
	}

	// splits the input on every match, like String.split
	public static List<String> split(Nfa machine, String input) {
		List<String> pieces = new ArrayList<>();
		int last = 0;
		int from = 0;
		boolean matched = false;
		while (from <= input.length()) {
			Match match = find(machine, input, from);
			if (match == null) {
				break;
			}
			if (match.start() == match.end() && !matched && match.start() == 0) {
				from = match.end() + 1; // an empty leading match contributes no piece
				continue;
			}
			pieces.add(input.substring(last, match.start()));
			matched = true;
			last = match.end();
			from = match.end() == match.start() ? match.end() + 1 : match.end();
		}

		if (!matched) {
			pieces.add(input);
			return pieces;
		}

		pieces.add(input.substring(last));
		while (!pieces.isEmpty() && pieces.getLast().isEmpty()) {
			pieces.removeLast();
		}
		return pieces;
	}

	private static CandidateList step(CandidateList current, char c, int position, int length) {
		CandidateList next = new CandidateList();
		for (Candidate candidate : current.list()) {
			State s = candidate.state();
			if (s.next() != null && s.set().contains(c)) {
				addCandidate(next, new Candidate(s.next(), candidate.start(), candidate.slots()), position, length);
			}
		}
		return next;
	}

	private static void addCandidate(CandidateList list, Candidate candidate, int position, int length) {
		State s = candidate.state();

		if (s.assertion() != null && !holds(s.assertion(), position, length)) {
			return;
		}

		if (s.saveSlot() != -1) {
			int[] slots = candidate.slots().clone();
			slots[s.saveSlot()] = position;
			candidate = new Candidate(s, candidate.start(), slots);
		}

		if (!list.add(candidate)) {
			return;
		}

		for (State target : s.epsilon()) {
			addCandidate(list, new Candidate(target, candidate.start(), candidate.slots()), position, length);
		}
	}

	private static int[] freshSlots(Nfa machine) {
		int[] slots = new int[machine.groupCount * 2];
		Arrays.fill(slots, -1);
		return slots;
	}

	private static boolean holds(Assertion kind, int position, int length) {
		return (kind == Assertion.START && position == 0) || (kind == Assertion.END && position == length);
	}
}
