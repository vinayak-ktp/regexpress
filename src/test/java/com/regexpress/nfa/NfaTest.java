package com.regexpress.nfa;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.report;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.regexpress.ast.Node;
import com.regexpress.parser.Parser;

public class NfaTest {
	public static void main(String[] args) {
		String[] patterns = { "a|bcd|e*", "a+b?", "[a-c][^x]", "a{2,4}", "(ab){2}", "a**",
				"\\d\\w\\s", "[\\d\\W]", "a\\*\\.b", "\\D+", "^a$", "a$b" };

		for (String pattern : patterns) {
			checkInvariants(pattern);
		}

		report();
	}

	private static void checkInvariants(String pattern) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		List<State> states = machine.allStates;

		check("a machine for \"" + pattern + "\" has at most 2 * pattern length states", true, states.size() <= 2 * pattern.length());
		check("a machine for \"" + pattern + "\" must have only one accepting state", (int) states.stream().filter(s -> s.accepting).count(), 1);

		Set<State> reachable = reachableStates(machine.start);

		check("every state in a machine for \"" + pattern + "\" is reachable from start", reachable.size(), states.size());
		check("the accepting state of a machine for \"" + pattern + "\" is reachable from start", true, states.stream().filter(s -> s.accepting).allMatch(reachable::contains));

		boolean hasUnexpectedDeadEnd = false;
		for (State s : states) {
			if (s.next == null && s.epsilon.isEmpty() && !s.accepting) {
				hasUnexpectedDeadEnd = true;
			}
		}
		check("no state in a machine for \"" + pattern + "\" is a dangling dead end", hasUnexpectedDeadEnd, false);
	}

	// walk every arrow reachable from start using stack
	private static Set<State> reachableStates(State start) {
		Set<State> visited = new HashSet<>();
		Deque<State> toVisit = new ArrayDeque<>();
		toVisit.push(start);

		while (!toVisit.isEmpty()) {
			State s = toVisit.pop();
			if (!visited.add(s)) {
				continue;
			}
			if (s.next != null) {
				toVisit.push(s.next);
			}
			for (State target : s.epsilon) {
				toVisit.push(target);
			}
		}
		return visited;
	}
}
