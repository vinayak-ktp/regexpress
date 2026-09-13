package com.regexpress.nfa;

import java.util.ArrayList;
import java.util.List;

public final class State {

	int id;

	// labelled arrow
	char label;
	State next;

	// free arrows
	List<State> epsilon = new ArrayList<>();

	// final state of the machine
	boolean accepting;
}