package com.regexpress.nfa;

import java.util.ArrayList;
import java.util.List;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.CharSetNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.EmptyNode;
import com.regexpress.ast.EndAnchorNode;
import com.regexpress.ast.GroupNode;
import com.regexpress.ast.Node;
import com.regexpress.ast.OptionalNode;
import com.regexpress.ast.PlusNode;
import com.regexpress.ast.StarNode;
import com.regexpress.ast.StartAnchorNode;

public final class NfaBuilder {

	private final List<State> allStates = new ArrayList<>();
	private int groupCount;

	private NfaBuilder() { }

	public static Nfa build(Node ast) {
		NfaBuilder builder = new NfaBuilder();
		Fragment machine = builder.buildFragment(ast);
		machine.exit.accepting = true;
		return new Nfa(machine.entrance, builder.allStates, builder.groupCount);
	}

	private State newState() {
		State state = new State();
		state.id = allStates.size();
		allStates.add(state);
		return state;
	}

	private Fragment buildFragment(Node node) {
		return switch (node) {
			case CharSetNode(CharSet set) -> buildCharSet(set);
			case ConcatNode(Node left, Node right) -> buildConcat(buildFragment(left), buildFragment(right));
			case AlternateNode(Node left, Node right) -> buildAlternate(buildFragment(left), buildFragment(right));
			case StarNode(Node child) -> buildStar(buildFragment(child));
			case PlusNode(Node child) -> buildPlus(buildFragment(child));
			case OptionalNode(Node child) -> buildOptional(buildFragment(child));
			case GroupNode(Node child, int index) -> buildGroup(buildFragment(child), index);
			case StartAnchorNode() -> buildAssertion(Assertion.START);
			case EndAnchorNode() -> buildAssertion(Assertion.END);
			case EmptyNode() -> buildEmpty();
		};
	}

	private Fragment buildCharSet(CharSet set) {
		State in = newState();
		State out = newState();
		in.set = set;
		in.next = out;
		return new Fragment(in, out);
	}

	private Fragment buildAssertion(Assertion kind) {
		State in = newState();
		State out = newState();
		in.assertion = kind;
		in.epsilon.add(out);
		return new Fragment(in, out);
	}

	private Fragment buildEmpty() {
		State in = newState();
		State out = newState();
		in.epsilon.add(out);
		return new Fragment(in, out);
	}

	private Fragment buildGroup(Fragment child, int index) {
		State in = newState();
		State out = newState();

		groupCount = Math.max(groupCount, index + 1);

		in.saveSlot = 2 * index;
		out.saveSlot = 2 * index + 1;

		in.epsilon.add(child.entrance);
		child.exit.epsilon.add(out);

		return new Fragment(in, out);
	}

	private Fragment buildConcat(Fragment left, Fragment right) {
		left.exit.epsilon.add(right.entrance);
		return new Fragment(left.entrance, right.exit);
	}

	private Fragment buildAlternate(Fragment left, Fragment right) {
		State in = newState();
		State out = newState();

		in.epsilon.add(left.entrance);
		in.epsilon.add(right.entrance);

		left.exit.epsilon.add(out);
		right.exit.epsilon.add(out);

		return new Fragment(in, out);
	}

	private Fragment buildStar(Fragment child) {
		State in = newState();
		State out = newState();

		in.epsilon.add(child.entrance);
		child.exit.epsilon.add(in);
		child.exit.epsilon.add(out);
		in.epsilon.add(out);

		return new Fragment(in, out);
	}

	private Fragment buildPlus(Fragment child) {
		State in = newState();
		State out = newState();

		in.epsilon.add(child.entrance);
		child.exit.epsilon.add(child.entrance);
		child.exit.epsilon.add(out);

		return new Fragment(in, out);
	}

	private Fragment buildOptional(Fragment child) {
		State in = newState();
		State out = newState();

		in.epsilon.add(child.entrance);
		child.exit.epsilon.add(out);
		in.epsilon.add(out);

		return new Fragment(in, out);
	}
}
