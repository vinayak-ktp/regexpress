package com.regexpress.matcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.regexpress.nfa.Assertion;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.State;

// answers the yes/no question deterministically: each state set remembers its successor
// per character, so a character costs one table lookup instead of a fresh epsilon closure
public final class LazyDfaMatcher implements Matcher {

	// the most state sets to remember before computing successors without caching
	private static final int CACHE_LIMIT = 10_000;

	private final Nfa machine;
	private final ConcurrentMap<Set<State>, DfaState> cache = new ConcurrentHashMap<>();

	LazyDfaMatcher(Nfa machine) {
		this.machine = machine;
	}

	@Override
	public boolean matches(String input) {
		int length = input.length();
		if (length < machine.minLength) {
			return false;
		}

		// the start position allows the start anchor, the end anchor only on empty input
		Set<State> start = closure(Set.of(machine.start), true, length == 0);
		if (length == 0) {
			return accepting(start);
		}

		DfaState current = dfaState(start);
		for (int i = 0; i < length - 1; i++) {
			current = current.on(input.charAt(i));
			if (current.states().isEmpty()) {
				return false;
			}
		}

		// the final position allows the end anchor, so the last step bypasses the cache
		return accepting(closure(step(current.states(), input.charAt(length - 1)), false, true));
	}

	private final class DfaState {

		private final Set<State> states;
		private final ConcurrentMap<Character, DfaState> transitions = new ConcurrentHashMap<>();

		DfaState(Set<State> states) {
			this.states = states;
		}

		Set<State> states() {
			return states;
		}

		DfaState on(char c) {
			DfaState next = transitions.get(c);
			if (next == null) {
				next = dfaState(closure(step(states, c), false, false));
				DfaState won = transitions.putIfAbsent(c, next);
				if (won != null) {
					next = won;
				}
			}
			return next;
		}
	}

	private DfaState dfaState(Set<State> states) {
		DfaState state = cache.get(states);
		if (state != null) {
			return state;
		}
		if (cache.size() >= CACHE_LIMIT) {
			return new DfaState(states);
		}
		state = new DfaState(states);
		DfaState won = cache.putIfAbsent(states, state);
		return won != null ? won : state;
	}

	// all states reachable via epsilon, except those behind a failing assertion
	private static Set<State> closure(Set<State> states, boolean startAllowed, boolean endAllowed) {
		Set<State> visited = new HashSet<>();
		Deque<State> toVisit = new ArrayDeque<>(states);

		while (!toVisit.isEmpty()) {
			State s = toVisit.pop();

			if (s.assertion() != null && !holds(s.assertion(), startAllowed, endAllowed)) {
				continue;
			}

			if (visited.add(s)) {
				for (State target : s.epsilon()) {
					toVisit.push(target);
				}
			}
		}
		return visited;
	}

	// the states reached by following the labelled arrow on this character
	private static Set<State> step(Set<State> states, char c) {
		Set<State> next = new HashSet<>();
		for (State s : states) {
			if (s.next() != null && s.set().contains(c)) {
				next.add(s.next());
			}
		}
		return next;
	}

	private static boolean accepting(Set<State> states) {
		for (State s : states) {
			if (s.accepting()) {
				return true;
			}
		}
		return false;
	}

	private static boolean holds(Assertion kind, boolean startAllowed, boolean endAllowed) {
		return (kind == Assertion.START && startAllowed) || (kind == Assertion.END && endAllowed);
	}
}
