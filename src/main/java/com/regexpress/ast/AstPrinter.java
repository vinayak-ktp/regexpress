package com.regexpress.ast;

public final class AstPrinter {

	private AstPrinter() { }

	public static String flat(Node node) {
		return switch (node) {
			case CharNode(char value) -> String.valueOf(value);
			case ConcatNode(Node left, Node right) -> "Concat(" + flat(left) + ", " + flat(right) + ")";
			case AlternateNode(Node left, Node right) -> "Alternate(" + flat(left) + ", " + flat(right) + ")";
			case StarNode(Node child) -> "Star(" + flat(child) + ")";
			case EmptyNode() -> "Empty";
		};
	}

	public static String tree(Node node) {
		return tree(node, 0);
	}

	static String tree(Node node, int depth) {
		return switch (node) {
			case CharNode(char value) -> "\t".repeat(depth) + String.valueOf(value);
			case ConcatNode(Node left, Node right) -> "\t".repeat(depth) + "Concat\n" + tree(left, depth+1) + "\n" + tree(right, depth+1);
			case AlternateNode(Node left, Node right) -> "\t".repeat(depth) + "Alternate\n" + tree(left, depth+1) + "\n" + tree(right, depth+1);
			case StarNode(Node child) -> "\t".repeat(depth) + "Star\n" + tree(child, depth+1);
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
