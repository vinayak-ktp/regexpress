package com.regexpress.matcher;

import com.regexpress.nfa.Nfa;

// picks which matcher answers the yes/no question for a machine
public final class MatcherFactory {

	private MatcherFactory() { }

	public static Matcher forMachine(Nfa machine) {
		return new NfaMatcher(machine);
	}
}
