package com.regexpress.nfa;

import java.util.List;

public final class Nfa {
	public final State start;
	List<State> allStates;

	Nfa(State start, List<State> allStates) {
		this.start = start;
		this.allStates = allStates;
	}
}