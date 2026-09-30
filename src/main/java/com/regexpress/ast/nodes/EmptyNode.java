package com.regexpress.ast.nodes;

public record EmptyNode() implements Node {
	@Override
	public String toString() {
		return "Empty";
	}
}