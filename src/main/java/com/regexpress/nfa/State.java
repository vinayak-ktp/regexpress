package com.regexpress.nfa;

import java.util.ArrayList;
import java.util.List;

import com.regexpress.ast.CharSet;

public final class State {

	int id;
	int saveSlot = -1;

	// labelled arrow
	CharSet set;
	State next;

	Assertion assertion;

	// free arrows
	List<State> epsilon = new ArrayList<>();

	// final state of the machine
	boolean accepting;

	public int saveSlot() {
		return saveSlot;
	}

	public CharSet set() {
		return set;
	}

	public State next() {
		return next;
	}

	public Assertion assertion() {
		return assertion;
	}

	public List<State> epsilon() {
		return epsilon;
	}

	public boolean accepting() {
		return accepting;
	}
}