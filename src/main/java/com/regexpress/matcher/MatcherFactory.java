package com.regexpress.matcher;

import com.regexpress.nfa.Nfa;

// picks which matcher answers the yes/no question for a machine
public final class MatcherFactory {

	private MatcherFactory() { }

	public static Matcher forMachine(Nfa machine) {
		if (machine.stateCount() <= 64) {
			return new BitDfaMatcher(machine);
		}
		return new LazyDfaMatcher(machine);
	}
}
