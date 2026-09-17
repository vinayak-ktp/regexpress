package com.regexpress.nfa;

import java.util.ArrayList;
import java.util.List;

import com.regexpress.ast.CharSet;

public final class State {

	int id;

	// labelled arrow
	CharSet set;
	State next;

	// free arrows
	List<State> epsilon = new ArrayList<>();

	// final state of the machine
	boolean accepting;

	public CharSet set() {
		return set;
	}

	public State next() {
		return next;
	}

	public List<State> epsilon() {
		return epsilon;
	}

	public boolean accepting() {
		return accepting;
	}
}