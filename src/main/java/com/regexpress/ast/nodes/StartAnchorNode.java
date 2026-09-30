package com.regexpress.ast.nodes;

public record StartAnchorNode() implements Node {
	@Override
	public String toString() {
		return "^";
	}
}
