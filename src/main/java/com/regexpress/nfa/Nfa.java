package com.regexpress.nfa;

import java.util.List;

import com.regexpress.ast.CharSet;

public final class Nfa {
	public final State start;
	public final int groupCount;
	// prefilter facts computed from the tree, used to skip positions no match can start at
	public final CharSet firstChars;
	public final String literalPrefix;
	public final int minLength;
	public final boolean nullable;
	List<State> allStates;

	Nfa(State start, List<State> allStates, int groupCount, CharSet firstChars, String literalPrefix, int minLength) {
		this.start = start;
		this.allStates = allStates;
		this.groupCount = groupCount;
		this.firstChars = firstChars;
		this.literalPrefix = literalPrefix;
		this.minLength = minLength;
		this.nullable = minLength == 0;
	}

	// the number of states in the machine
	public int stateCount() {
		return allStates.size();
	}

	// the state at the given position in the machine's state list
	public State state(int id) {
		return allStates.get(id);
	}
}