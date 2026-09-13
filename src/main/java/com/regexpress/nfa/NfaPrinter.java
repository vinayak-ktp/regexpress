package com.regexpress.nfa;

import java.util.stream.Collectors;

public final class NfaPrinter {

	private NfaPrinter() { }

	public static void print(Nfa machine) {
		for (State s : machine.allStates) {
			String line = "State " + String.format("%3d", s.id);

			if (s.next != null) {
				line += "  ——" + s.label + "——>  " + s.next.id;
			} else if (!s.epsilon.isEmpty()) {
				line += "  ——ε——>  " + s.epsilon.stream().map(e -> String.valueOf(e.id)).collect(Collectors.joining(", "));
			}

			if (s.accepting) {
				line += " (accepting)";
			}

			System.out.println(line);
		}
	}
}
