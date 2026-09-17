package com.regexpress.ast;

import java.util.Objects;

public record PlusNode(Node child) implements Node {
	public PlusNode {
		Objects.requireNonNull(child, "child must not be null");
	}

	@Override
	public String toString() {
		return "Plus(" + child.toString() + ")";
	}
}
