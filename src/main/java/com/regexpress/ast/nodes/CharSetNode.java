package com.regexpress.ast.nodes;

import com.regexpress.ast.CharSet;

public record CharSetNode(CharSet set) implements Node {
	@Override
	public String toString() {
		String s = set.toString();
		return (s.length() == 1 ? "" : "CharSet") + s;
	}
}
