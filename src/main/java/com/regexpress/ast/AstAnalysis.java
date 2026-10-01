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

// static facts about a pattern's tree: what a match can start with, must start with, and how short it can be
public final class AstAnalysis {

	private AstAnalysis() { }

	// the fewest characters any match spans
	public static int minLength(Node node) {
		return switch (node) {
			case EmptyNode n -> 0;
			case StartAnchorNode n -> 0;
			case EndAnchorNode n -> 0;
			case CharSetNode n -> 1;
			case ConcatNode n -> minLength(n.left()) + minLength(n.right());
			case AlternateNode n -> Math.min(minLength(n.left()), minLength(n.right()));
			case StarNode n -> 0;
			case PlusNode n -> minLength(n.child());
			case OptionalNode n -> 0;
			case GroupNode n -> minLength(n.child());
		};
	}

	// true when the pattern can match the empty string
	public static boolean nullable(Node node) {
		return minLength(node) == 0;
	}

	// the characters a non-empty match can begin with
	public static CharSet firstChars(Node node) {
		return switch (node) {
			case EmptyNode n -> CharSet.empty();
			case StartAnchorNode n -> CharSet.empty();
			case EndAnchorNode n -> CharSet.empty();
			case CharSetNode n -> n.set();
			case ConcatNode n -> nullable(n.left())
				? union(firstChars(n.left()), firstChars(n.right()))
				: firstChars(n.left());
			case AlternateNode n -> union(firstChars(n.left()), firstChars(n.right()));
			case StarNode n -> firstChars(n.child());
			case PlusNode n -> firstChars(n.child());
			case OptionalNode n -> firstChars(n.child());
			case GroupNode n -> firstChars(n.child());
		};
	}

	// the longest literal string every match must begin with ("" when there is none)
	public static String literalPrefix(Node node) {
		String exact = literal(node);
		if (exact != null) {
			return exact;
		}
		return switch (node) {
			case ConcatNode n -> {
				String left = literal(n.left());
				yield left != null ? left + literalPrefix(n.right()) : literalPrefix(n.left());
			}
			case AlternateNode n -> commonPrefix(literalPrefix(n.left()), literalPrefix(n.right()));
			case GroupNode n -> literalPrefix(n.child());
			default -> "";
		};
	}

	// the exact string this node matches, or null when it can match more than one string
	private static String literal(Node node) {
		return switch (node) {
			case EmptyNode n -> "";
			case StartAnchorNode n -> null;
			case EndAnchorNode n -> null;
			case CharSetNode n -> n.set().isSingleton() ? String.valueOf(n.set().singleChar()) : null;
			case ConcatNode n -> combine(literal(n.left()), literal(n.right()));
			case AlternateNode n -> {
				String left = literal(n.left());
				String right = literal(n.right());
				yield left != null && left.equals(right) ? left : null;
			}
			case StarNode n -> null;
			case PlusNode n -> null;
			case OptionalNode n -> null;
			case GroupNode n -> literal(n.child());
		};
	}

	// unions two sets into a fresh one, leaving the inputs untouched
	private static CharSet union(CharSet left, CharSet right) {
		CharSet set = CharSet.empty();
		set.union(left);
		set.union(right);
		return set;
	}

	private static String combine(String left, String right) {
		return left == null || right == null ? null : left + right;
	}

	private static String commonPrefix(String a, String b) {
		int limit = Math.min(a.length(), b.length());
		int i = 0;
		while (i < limit && a.charAt(i) == b.charAt(i)) {
			i++;
		}
		return a.substring(0, i);
	}
}
