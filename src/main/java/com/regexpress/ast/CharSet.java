package com.regexpress.ast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CharSet {

	final List<Range> rangeList;
	boolean negated = false;

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

	public CharSet negate() {
		// immutable once negated
		if (negated) return this;
		negated = true;
		return this;
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

	public static CharSet digit() {
		return range('0', '9');
	}

	public static CharSet word() {
		CharSet set = range('a', 'z');
		set.union(range('A', 'Z'));
		set.union(range('0', '9'));
		set.union(of('_'));
		return set;
	}

	public static CharSet whitespace() {
		CharSet set = new CharSet(true);
		String whitespaces = " \t\n\r\f";
		for (char c : whitespaces.toCharArray()) set.union(of(c));
		return set;
	}

	public static CharSet fromShorthand(char kind) {
		return switch(kind) {
			case 'd' -> digit();
			case 'D' -> digit().negate();
			case 'w' -> word();
			case 'W' -> word().negate();
			case 's' -> whitespace();
			case 'S' -> whitespace().negate();
			default -> throw new IllegalArgumentException("not a class shorthand: " + kind);
		};
	}

	public void union(CharSet other) {
		if (other.negated) {
			rangeList.addAll(complement(other.rangeList));
		} else {
			rangeList.addAll(other.rangeList);
		}
	}

	private static List<Range> complement(List<Range> ranges) {
		List<Range> sorted = new ArrayList<>(ranges);
		sorted.sort(Comparator.comparingInt(Range::from));

		List<Range> gaps = new ArrayList<>();
		int next = Character.MIN_VALUE;
		for (Range r : sorted) {
			if (r.from() > next) {
				gaps.add(new Range((char) next, (char) (r.from() - 1)));
			}
			next = Math.max(next, r.to() + 1);
			if (next > Character.MAX_VALUE) return gaps;
		}
		gaps.add(new Range((char) next, Character.MAX_VALUE));
		return gaps;
	}

	public boolean contains(char c) {
		boolean inRange = false;
		for (Range r : rangeList) {
			if (c >= r.from() && c <= r.to()) {
				inRange = true;
				break;
			}
		}
		return negated != inRange;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Range r : rangeList) sb.append(getRange(r));
		String s = sb.toString();
		if (negated) return "[^" + s + "]";
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
		return negated == other.negated && rangeList.equals(other.rangeList);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(rangeList, negated);
	}
}

record Range(char from, char to) { }