package com.regexpress.ast;

import java.util.Objects;

public record PlusNode(Node child, boolean lazy) implements Node {
	public PlusNode {
		Objects.requireNonNull(child, "child must not be null");
	}

	@Override
	public String toString() {
		return lazy ? "Plus(" + child.toString() + ", lazy)" : "Plus(" + child.toString() + ")";
	}
}
