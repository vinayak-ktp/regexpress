package com.regexpress.ast;

import java.util.Objects;

public record OptionalNode(Node child) implements Node {
	public OptionalNode {
		Objects.requireNonNull(child, "child must not be null");
	}

	@Override
	public String toString() {
		return "Optional(" + child.toString() + ")";
	}
}
