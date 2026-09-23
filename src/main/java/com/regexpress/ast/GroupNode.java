package com.regexpress.ast;

public record GroupNode(Node child, int index) implements Node {
	@Override
	public String toString() {
		return "Group(" + child.toString() + ")";
	}
}
