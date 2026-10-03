package com.regexpress.ast;

import com.regexpress.ast.nodes.AlternateNode;
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

// pattern rewrites that shrink the tree without changing what it matches
public final class AstOptimiser {

	private AstOptimiser() { }

	public static Node optimise(Node node) {
		return switch (node) {
			case CharSetNode n -> n;
			case ConcatNode(Node left, Node right) -> concat(optimise(left), optimise(right));
			case AlternateNode(Node left, Node right) -> alternate(optimise(left), optimise(right));
			case StarNode(Node child, boolean lazy) -> star(optimise(child), lazy);
			case PlusNode(Node child, boolean lazy) -> plus(optimise(child), lazy);
			case OptionalNode(Node child, boolean lazy) -> optional(optimise(child), lazy);
			case GroupNode(Node child, int index) -> new GroupNode(optimise(child), index);
			case EmptyNode n -> n;
			case StartAnchorNode n -> n;
			case EndAnchorNode n -> n;
		};
	}

	private static Node concat(Node left, Node right) {
		if (left instanceof EmptyNode) {
			return right;
		}
		if (right instanceof EmptyNode) {
			return left;
		}
		return new ConcatNode(left, right);
	}

	private static Node alternate(Node left, Node right) {
		if (left.equals(right)) {
			return left;
		}
		if (left instanceof CharSetNode l && right instanceof CharSetNode r) {
			return new CharSetNode(union(l.set(), r.set()));
		}
		return new AlternateNode(left, right);
	}

	// fused quantifiers collapse only when both are greedy: lazy nesting keeps its own preference
	private static Node star(Node child, boolean lazy) {
		if (child instanceof EmptyNode) {
			return child;
		}
		if (!lazy && child instanceof StarNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		if (!lazy && child instanceof PlusNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		if (!lazy && child instanceof OptionalNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		return new StarNode(child, lazy);
	}

	private static Node plus(Node child, boolean lazy) {
		if (child instanceof EmptyNode) {
			return child;
		}
		if (!lazy && child instanceof StarNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		if (!lazy && child instanceof OptionalNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		if (!lazy && child instanceof PlusNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new PlusNode(inner, false);
		}
		return new PlusNode(child, lazy);
	}

	private static Node optional(Node child, boolean lazy) {
		if (child instanceof EmptyNode) {
			return child;
		}
		if (!lazy && child instanceof StarNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		if (!lazy && child instanceof PlusNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new StarNode(inner, false);
		}
		if (!lazy && child instanceof OptionalNode(Node inner, boolean innerLazy) && !innerLazy) {
			return new OptionalNode(inner, false);
		}
		return new OptionalNode(child, lazy);
	}

	// unions two sets into a fresh one, leaving the inputs untouched
	private static CharSet union(CharSet left, CharSet right) {
		CharSet set = CharSet.empty();
		set.union(left);
		set.union(right);
		return set;
	}
}
