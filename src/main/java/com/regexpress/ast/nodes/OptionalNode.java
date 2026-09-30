package com.regexpress.ast.nodes;

import java.util.Objects;

public record OptionalNode(Node child, boolean lazy) implements Node {
	public OptionalNode {
		Objects.requireNonNull(child, "child must not be null");
	}

	@Override
	public String toString() {
		return lazy ? "Optional(" + child.toString() + ", lazy)" : "Optional(" + child.toString() + ")";
	}
}
