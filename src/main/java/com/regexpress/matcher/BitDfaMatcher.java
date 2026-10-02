package com.regexpress.matcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.regexpress.nfa.Assertion;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.State;

// answers the yes/no question with the state set packed into one long (at most 64 states):
// closures come from precomputed per-state masks and a cached transition is one array read
public final class BitDfaMatcher implements Matcher {

	// the most state sets to remember before computing successors without caching
	private static final int CACHE_LIMIT = 10_000;

	private final Nfa machine;
	private final State[] states;
	private final int startId;
	private final long acceptingMask;
	private final long[] startClosure;
	private final long[] middleClosure;
	private final long[] endClosure;
	private final long[] bothClosure;
	private final ConcurrentMap<Long, DfaState> cache = new ConcurrentHashMap<>();

	BitDfaMatcher(Nfa machine) {
		if (machine.stateCount() > 64) {
			throw new IllegalArgumentException("bit-parallel matching needs at most 64 states");
		}
		this.machine = machine;
		this.states = new State[machine.stateCount()];
		for (int i = 0; i < states.length; i++) {
			states[i] = machine.state(i);
		}
		this.startId = machine.start.id();
		long accepting = 0;
		for (State s : states) {
			if (s.accepting()) {
				accepting |= 1L << s.id();
			}
		}
		this.acceptingMask = accepting;
		this.startClosure = closureTable(true, false);
		this.middleClosure = closureTable(false, false);
		this.endClosure = closureTable(false, true);
		this.bothClosure = closureTable(true, true);
	}

	@Override
	public boolean matches(String input) {
		int length = input.length();
		if (length < machine.minLength) {
			return false;
		}
		if (length == 0) {
			long start = closure(1L << startId, bothClosure);
			return (start & acceptingMask) != 0;
		}
		DfaState current = dfaState(closure(1L << startId, startClosure));
		for (int i = 0; i < length - 1; i++) {
			current = current.on(input.charAt(i));
			if (current.mask() == 0) {
				return false;
			}
		}

		// the final position allows the end anchor, so the last step bypasses the cache
		long last = closure(step(current.mask(), input.charAt(length - 1)), endClosure);
		return (last & acceptingMask) != 0;
	}

	private final class DfaState {

		private final long mask;
		private final DfaState[] ascii = new DfaState[128];
		private final ConcurrentMap<Character, DfaState> wide = new ConcurrentHashMap<>();

		DfaState(long mask) {
			this.mask = mask;
		}

		long mask() {
			return mask;
		}

		DfaState on(char c) {
			DfaState target;
			if (c < 128) {
				target = ascii[c];
				if (target == null) {
					target = dfaState(closure(step(mask, c), middleClosure));
					ascii[c] = target;
				}
				return target;
			}
			target = wide.get(c);
			if (target == null) {
				target = dfaState(closure(step(mask, c), middleClosure));
				DfaState won = wide.putIfAbsent(c, target);
				if (won != null) {
					target = won;
				}
			}
			return target;
		}
	}

	private DfaState dfaState(long mask) {
		DfaState state = cache.get(mask);
		if (state != null) {
			return state;
		}
		if (cache.size() >= CACHE_LIMIT) {
			return new DfaState(mask);
		}
		state = new DfaState(mask);
		DfaState won = cache.putIfAbsent(mask, state);
		return won != null ? won : state;
	}

	// the states reached by following the labelled arrow on this character
	private long step(long mask, char c) {
		long next = 0;
		long remaining = mask;
		while (remaining != 0) {
			int id = Long.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			State s = states[id];
			if (s.next() != null && s.set().contains(c)) {
				next |= 1L << s.next().id();
			}
		}
		return next;
	}

	// expands each state in the mask to its precomputed closure under one context
	private long closure(long mask, long[] table) {
		long result = 0;
		long remaining = mask;
		while (remaining != 0) {
			int id = Long.numberOfTrailingZeros(remaining);
			remaining &= remaining - 1;
			result |= table[id];
		}
		return result;
	}

	private long[] closureTable(boolean startAllowed, boolean endAllowed) {
		long[] table = new long[states.length];
		for (int i = 0; i < states.length; i++) {
			table[i] = closureOf(states[i], startAllowed, endAllowed);
		}
		return table;
	}

	private long closureOf(State start, boolean startAllowed, boolean endAllowed) {
		long mask = 0;
		Deque<State> toVisit = new ArrayDeque<>();
		toVisit.push(start);
		while (!toVisit.isEmpty()) {
			State s = toVisit.pop();
			if (s.assertion() != null && !holds(s.assertion(), startAllowed, endAllowed)) {
				continue;
			}
			long bit = 1L << s.id();
			if ((mask & bit) == 0) {
				mask |= bit;
				for (State target : s.epsilon()) {
					toVisit.push(target);
				}
			}
		}
		return mask;
	}

	private static boolean holds(Assertion kind, boolean startAllowed, boolean endAllowed) {
		return (kind == Assertion.START && startAllowed) || (kind == Assertion.END && endAllowed);
	}
}
