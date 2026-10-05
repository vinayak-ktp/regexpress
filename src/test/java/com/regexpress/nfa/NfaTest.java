package com.regexpress.nfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.regexpress.ast.nodes.Node;
import com.regexpress.parser.Parser;

// structural invariants that must hold for the machine built from any pattern,
// checked separately per invariant so a failure names exactly which one broke
class NfaTest {

	static Stream<String> patterns() {
		return Stream.of("a|bcd|e*", "a+b?", "[a-c][^x]", "a{2,4}", "(ab){2}", "a**",
				"\\d\\w\\s", "[\\d\\W]", "a\\*\\.b", "\\D+", "^a$", "a$b");
	}

	private static Nfa build(String pattern) {
		Node ast = Parser.parse(pattern);
		return NfaBuilder.build(ast);
	}

	@ParameterizedTest(name = "a machine for \"{0}\" has at most 2 * pattern length states")
	@MethodSource("patterns")
	void stateCountIsBounded(String pattern) {
		assertTrue(build(pattern).allStates.size() <= 2 * pattern.length());
	}

	@ParameterizedTest(name = "a machine for \"{0}\" must have only one accepting state")
	@MethodSource("patterns")
	void exactlyOneAcceptingState(String pattern) {
		long accepting = build(pattern).allStates.stream().filter(s -> s.accepting).count();
		assertEquals(1, accepting);
	}

	@ParameterizedTest(name = "every state in a machine for \"{0}\" is reachable from start")
	@MethodSource("patterns")
	void everyStateIsReachable(String pattern) {
		Nfa machine = build(pattern);
		assertEquals(machine.allStates.size(), reachableStates(machine.start).size());
	}

	@ParameterizedTest(name = "the accepting state of a machine for \"{0}\" is reachable from start")
	@MethodSource("patterns")
	void acceptingStateIsReachable(String pattern) {
		Nfa machine = build(pattern);
		Set<State> reachable = reachableStates(machine.start);
		assertTrue(machine.allStates.stream().filter(s -> s.accepting).allMatch(reachable::contains));
	}

	@ParameterizedTest(name = "no state in a machine for \"{0}\" is a dangling dead end")
	@MethodSource("patterns")
	void noDanglingDeadEnds(String pattern) {
		List<State> states = build(pattern).allStates;
		boolean hasUnexpectedDeadEnd = false;
		for (State s : states) {
			if (s.next == null && s.epsilon.isEmpty() && !s.accepting) {
				hasUnexpectedDeadEnd = true;
			}
		}
		assertFalse(hasUnexpectedDeadEnd);
	}

	// walk every arrow reachable from start using a stack
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
