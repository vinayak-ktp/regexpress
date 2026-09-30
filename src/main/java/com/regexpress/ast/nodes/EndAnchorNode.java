package com.regexpress.ast.nodes;

public record EndAnchorNode() implements Node {
	@Override
	public String toString() {
		return "$";
	}
}
