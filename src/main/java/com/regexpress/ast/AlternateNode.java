package com.regexpress.ast;

import java.util.Objects;

public record AlternateNode(Node left, Node right) implements Node {
	public AlternateNode {
		Objects.requireNonNull(left, "left must not be null");
		Objects.requireNonNull(right, "right must not be null");
	}

	@Override
	public String toString() {
		return "Alternate(" + left.toString() + ", " + right.toString() + ")";
	}
}