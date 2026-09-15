package com.regexpress;

import com.regexpress.ast.Node;
import com.regexpress.matcher.NfaMatcher;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

public class Regex {

	private final Nfa machine;

	private Regex(Nfa machine) {
		this.machine = machine;
	}

	public static Regex compile(String pattern) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		return new Regex(machine);
	}

	public boolean matches(String input) {
		return NfaMatcher.matches(machine, input);
	}

	// for one-off use only!!!
	public static boolean matches(String pattern, String input) {
		return compile(pattern).matches(input);
	}
}
