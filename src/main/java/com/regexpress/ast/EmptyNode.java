package com.regexpress.ast;

public record EmptyNode() implements Node {
	@Override
	public String toString() {
		return "Empty";
	}
}