package com.regexpress.nfa;

import java.util.List;

public final class Nfa {
	public final State start;
	public final int groupCount;
	List<State> allStates;

	Nfa(State start, List<State> allStates, int groupCount) {
		this.start = start;
		this.allStates = allStates;
		this.groupCount = groupCount;
	}
}