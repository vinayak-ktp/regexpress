package com.regexpress.matcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.regexpress.nfa.Assertion;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.State;

public final class NfaMatcher {

	private NfaMatcher() { }

	public static boolean matches(Nfa machine, String input) {
		int length = input.length();
		Set<State> current = epsilonClosure(Set.of(machine.start), 0, length);

		for (int i = 0; i < length; i++) {
			char c = input.charAt(i);
			Set<State> next = new HashSet<>();

			for (State s : current) {
				if (s.next() != null && s.set().contains(c)) {
					next.add(s.next());
				}
			}

			if (next.isEmpty()) {
				return false;
			}
			current = epsilonClosure(next, i + 1, length);
		}

		for (State s : current) {
			if (s.accepting()) {
				return true;
			}
		}
		return false;
	}

	// all states that can be reached freely (via epsilon) from a set of states,
	// except assertion states whose condition fails at the given position
	private static Set<State> epsilonClosure(Set<State> states, int position, int length) {
		Set<State> visited = new HashSet<>();
		Deque<State> toVisit = new ArrayDeque<>(states);

		while (!toVisit.isEmpty()) {
			State s = toVisit.pop();

			if (s.assertion() != null && !holds(s.assertion(), position, length)) {
				continue;
			}

			if (!visited.add(s)) {
				continue;
			}
			for (State target : s.epsilon()) {
				toVisit.push(target);
			}
		}
		return visited;
	}

	private static boolean holds(Assertion kind, int position, int length) {
		return (kind == Assertion.START && position == 0) || (kind == Assertion.END && position == length);
	}
}
