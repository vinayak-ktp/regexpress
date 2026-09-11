package com.regexpress.ast;

import java.util.Objects;

public record ConcatNode(Node left, Node right) implements Node {
	public ConcatNode {
		Objects.requireNonNull(left, "left must not be null");
		Objects.requireNonNull(right, "right must not be null");
	}

	@Override
	public String toString() {
		return "Concat(" + left.toString() + ", " + right.toString() + ")";
	}
}