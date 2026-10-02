package com.regexpress.nfa;

final class Fragment {
	final State entrance;
	final State exit;

	Fragment(State entrance, State exit) {
		this.entrance = entrance;
		this.exit = exit;
	}

	// wire this fragment's exit to the next fragment's entrance
	void connectTo(Fragment next) {
		exit.addEpsilon(next.entrance);
	}
}