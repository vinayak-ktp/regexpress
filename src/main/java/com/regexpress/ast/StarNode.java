package com.regexpress.ast;

import java.util.Objects;

public record StarNode(Node child) implements Node {
	public StarNode {
		Objects.requireNonNull(child, "child must not be null");
	}
}