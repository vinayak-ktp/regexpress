package com.regexpress.nfa;

import java.util.ArrayList;
import java.util.List;

import com.regexpress.ast.AstAnalysis;
import com.regexpress.ast.nodes.AlternateNode;
import com.regexpress.ast.CharSet;
import com.regexpress.ast.nodes.CharSetNode;
import com.regexpress.ast.nodes.ConcatNode;
import com.regexpress.ast.nodes.EmptyNode;
import com.regexpress.ast.nodes.EndAnchorNode;
import com.regexpress.ast.nodes.GroupNode;
import com.regexpress.ast.nodes.Node;
import com.regexpress.ast.nodes.OptionalNode;
import com.regexpress.ast.nodes.PlusNode;
import com.regexpress.ast.nodes.StarNode;
import com.regexpress.ast.nodes.StartAnchorNode;

public final class NfaBuilder {

	private final List<State> allStates = new ArrayList<>();
	private int groupCount;

	private NfaBuilder() { }

	public static Nfa build(Node ast) {
		NfaBuilder builder = new NfaBuilder();
		Fragment machine = builder.buildFragment(ast);
		machine.exit.accepting = true;
		return new Nfa(machine.entrance, builder.allStates, builder.groupCount,
				AstAnalysis.firstChars(ast), AstAnalysis.literalPrefix(ast), AstAnalysis.minLength(ast));
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
			case StarNode(Node child, boolean lazy) -> buildStar(buildFragment(child), lazy);
			case PlusNode(Node child, boolean lazy) -> buildPlus(buildFragment(child), lazy);
			case OptionalNode(Node child, boolean lazy) -> buildOptional(buildFragment(child), lazy);
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
		in.addEpsilon(out);
		return new Fragment(in, out);
	}

	private Fragment buildEmpty() {
		State in = newState();
		State out = newState();
		in.addEpsilon(out);
		return new Fragment(in, out);
	}

	private Fragment buildGroup(Fragment child, int index) {
		State in = newState();
		State out = newState();

		groupCount = Math.max(groupCount, index + 1);

		in.saveSlot = 2 * index;
		out.saveSlot = 2 * index + 1;

		in.addEpsilon(child.entrance);
		child.exit.addEpsilon(out);

		return new Fragment(in, out);
	}

	private Fragment buildConcat(Fragment left, Fragment right) {
		left.connectTo(right);
		return new Fragment(left.entrance, right.exit);
	}

	private Fragment buildAlternate(Fragment left, Fragment right) {
		State in = newState();
		State out = newState();

		in.addEpsilon(left.entrance);
		in.addEpsilon(right.entrance);

		left.exit.addEpsilon(out);
		right.exit.addEpsilon(out);

		return new Fragment(in, out);
	}

	private Fragment buildStar(Fragment child, boolean lazy) {
		State in = newState();
		State out = newState();

		if (lazy) {
			in.addEpsilon(out);
			in.addEpsilon(child.entrance);
			child.exit.addEpsilon(out);
			child.exit.addEpsilon(in);
		} else {
			in.addEpsilon(child.entrance);
			child.exit.addEpsilon(in);
			child.exit.addEpsilon(out);
			in.addEpsilon(out);
		}

		return new Fragment(in, out);
	}

	private Fragment buildPlus(Fragment child, boolean lazy) {
		State in = newState();
		State out = newState();

		in.addEpsilon(child.entrance);
		if (lazy) {
			child.exit.addEpsilon(out);
			child.exit.addEpsilon(child.entrance);
		} else {
			child.exit.addEpsilon(child.entrance);
			child.exit.addEpsilon(out);
		}

		return new Fragment(in, out);
	}

	private Fragment buildOptional(Fragment child, boolean lazy) {
		State in = newState();
		State out = newState();

		if (lazy) {
			in.addEpsilon(out);
			in.addEpsilon(child.entrance);
		} else {
			in.addEpsilon(child.entrance);
			in.addEpsilon(out);
		}
		child.exit.addEpsilon(out);

		return new Fragment(in, out);
	}
}
