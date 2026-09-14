package com.regexpress.matcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.State;

public final class NfaMatcher {

	private NfaMatcher() { }

	public static boolean matches(Nfa machine, String input) {
		Set<State> current = epsilonClosure(Set.of(machine.start));

		for (char c : input.toCharArray()) {
			Set<State> next = new HashSet<>();

			for (State s : current) {
				if (s.next() != null && s.label() == c) {
					next.add(s.next());
				}
			}

			if (next.isEmpty()) {
				return false;
			}
			current = epsilonClosure(next);
		}

		for (State s : current) {
			if (s.accepting()) {
				return true;
			}
		}
		return false;
	}

	// all states that can be reached freely (via epsilon) from a set of states
	private static Set<State> epsilonClosure(Set<State> states) {
		Set<State> visited = new HashSet<>();
		Deque<State> toVisit = new ArrayDeque<>(states);

		while (!toVisit.isEmpty()) {
			State s = toVisit.pop();
			if (!visited.add(s)) {
				continue;
			}
			for (State target : s.epsilon()) {
				toVisit.push(target);
			}
		}
		return visited;
	}
}
