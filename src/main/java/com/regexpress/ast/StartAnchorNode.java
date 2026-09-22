package com.regexpress.ast;

public record StartAnchorNode() implements Node {
	@Override
	public String toString() {
		return "^";
	}
}
