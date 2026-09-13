package com.regexpress.nfa;

final class Fragment {
	final State entrance;
	final State exit;

	Fragment(State entrance, State exit) {
		this.entrance = entrance;
		this.exit = exit;
	}
}