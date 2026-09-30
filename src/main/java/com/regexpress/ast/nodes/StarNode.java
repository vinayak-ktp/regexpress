package com.regexpress.ast.nodes;

import java.util.Objects;

public record StarNode(Node child, boolean lazy) implements Node {
	public StarNode {
		Objects.requireNonNull(child, "child must not be null");
	}

	@Override
	public String toString() {
		return lazy ? "Star(" + child.toString() + ", lazy)" : "Star(" + child.toString() + ")";
	}
}