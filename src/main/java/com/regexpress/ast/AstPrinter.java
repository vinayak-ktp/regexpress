package com.regexpress.ast;

public final class AstPrinter {

	private AstPrinter() { }

	// no flat-printing logic as it's already handled by the toString() methods
	public static String flat(Node node) {
		return node.toString();
	}

	public static String tree(Node node) {
		return tree(node, 0);
	}

	static String tree(Node node, int depth) {
		return switch (node) {
			case CharSetNode c -> "\t".repeat(depth) + c.toString();
			case ConcatNode(Node left, Node right) -> "\t".repeat(depth) + "Concat\n" + tree(left, depth+1) + "\n" + tree(right, depth+1);
			case AlternateNode(Node left, Node right) -> "\t".repeat(depth) + "Alternate\n" + tree(left, depth+1) + "\n" + tree(right, depth+1);
			case StarNode(Node child) -> "\t".repeat(depth) + "Star\n" + tree(child, depth+1);
			case PlusNode(Node child) -> "\t".repeat(depth) + "Plus\n" + tree(child, depth+1);
			case OptionalNode(Node child) -> "\t".repeat(depth) + "Optional\n" + tree(child, depth+1);
			case GroupNode(Node child, int index) -> "\t".repeat(depth) + "Group\n" + tree(child, depth+1);
			case StartAnchorNode() -> "\t".repeat(depth) + "^";
			case EndAnchorNode() -> "\t".repeat(depth) + "$";
			case EmptyNode() -> "\t".repeat(depth) + "Empty";
		};
	}

	public static void printFlat(Node node) {
		System.out.println(flat(node));
	}

	public static void printTree(Node node) {
		System.out.println(tree(node));
	}
}
