package com.regexpress.ast;

import java.util.ArrayList;
import java.util.List;

public class CharSet {

	final List<Range> rangeList;
	boolean negate = false;

	private CharSet(boolean empty) {
		 if (empty) rangeList = new ArrayList<>();
		 else rangeList = new ArrayList<>(List.of(new Range(Character.MIN_VALUE, Character.MAX_VALUE)));
	}

	private CharSet(char c) {
		rangeList = new ArrayList<>(List.of(new Range(c, c)));
	}

	private CharSet(char from, char to) {
		rangeList = new ArrayList<>(List.of(new Range(from, to)));
	}

	public void negate() {
		// immutable once negated
		if (negate) return;
		negate = true;
	}

	public static CharSet empty() {
		return new CharSet(true);
	}

	public static CharSet all() {
		return new CharSet(false);
	}

	public static CharSet of(char c) {
		return new CharSet(c);
	}

	public static CharSet range(char from, char to) {
		return new CharSet(from, to);
	}

	public void union(CharSet other) {
		rangeList.addAll(other.rangeList);
	}

	public boolean contains(char c) {
		boolean inRange = false;
		for (Range r : rangeList) {
			if (c >= r.from() && c <= r.to()) {
				inRange = true;
				break;
			}
		}
		return negate != inRange;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Range r : rangeList) sb.append(getRange(r));
		String s = sb.toString();
		if (negate) return "[^" + s + "]";
		return s.length() == 1 ? s : "[" + s + "]";
	}

	private String getRange(Range r) {
		if (r.from() == Character.MIN_VALUE && r.to() == Character.MAX_VALUE) return "all";
		if (r.from() == r.to()) return String.valueOf(r.from());
		return r.from() + "-" + r.to();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof CharSet other)) return false;
		return negate == other.negate && rangeList.equals(other.rangeList);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(rangeList, negate);
	}
}

record Range(char from, char to) { }