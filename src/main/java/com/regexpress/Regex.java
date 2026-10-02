package com.regexpress;

import java.util.List;

import com.regexpress.ast.nodes.Node;
import com.regexpress.matcher.Matcher;
import com.regexpress.matcher.MatcherFactory;
import com.regexpress.matcher.PikeMatcher;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.parser.Parser;

public class Regex {

	private final Nfa machine;
	private final Matcher matcher;

	private Regex(Nfa machine) {
		this.machine = machine;
		this.matcher = MatcherFactory.forMachine(machine);
	}

	public static Regex compile(String pattern) {
		Node ast = Parser.parse(pattern);
		Nfa machine = NfaBuilder.build(ast);
		return new Regex(machine);
	}

	public boolean matches(String input) {
		return matcher.matches(input);
	}

	// for one-off use only!!!
	public static boolean matches(String pattern, String input) {
		return compile(pattern).matches(input);
	}

	// the leftmost match, or null if the pattern does not occur
	public Match find(String input) {
		return find(input, 0);
	}

	// the leftmost match starting at or after `from`, or null
	public Match find(String input, int from) {
		com.regexpress.matcher.Match found = PikeMatcher.find(machine, input, from);
		return found == null ? null : new Match(input, found);
	}

	public String replaceAll(String input, String replacement) {
		return PikeMatcher.replaceAll(machine, input, replacement);
	}

	public List<String> split(String input) {
		return PikeMatcher.split(machine, input);
	}
}
