package com.regexpress.ast;

public record CharNode(char value) implements Node {
	@Override
	public String toString() {
		return String.valueOf(value);
	}
}