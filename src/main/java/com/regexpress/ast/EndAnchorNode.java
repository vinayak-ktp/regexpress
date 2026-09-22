package com.regexpress.ast;

public record EndAnchorNode() implements Node {
	@Override
	public String toString() {
		return "$";
	}
}
