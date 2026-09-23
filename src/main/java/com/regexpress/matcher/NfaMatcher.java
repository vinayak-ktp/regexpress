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
			Set<State> next = step(current, c);
			if (next.isEmpty()) return false;
			current = epsilonClosure(next, i + 1, length);
		}

		return hasAcceptingState(current);
	}

	private static Set<State> step(Set<State> states, char c) {
		Set<State> reachable = new HashSet<>();
		for (State s : states) {
			if (s.next() != null && s.set().contains(c)) {
				reachable.add(s.next());
			}
		}
		return reachable;
	}

	private static boolean hasAcceptingState(Set<State> states) {
		return states.stream().anyMatch(State::accepting);
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
			if (visited.contains(s)) {
				continue;
			}

			visited.add(s);
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
