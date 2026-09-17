package com.regexpress.ast;

public record CharSetNode(CharSet set) implements Node {
	@Override
	public String toString() {
		String s = set.toString();
		return (s.length() == 1 ? "" : "CharSet") + s;
	}
}
