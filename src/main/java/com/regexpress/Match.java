package com.regexpress;

// a match found by Regex.find: its span in the input and the text each group captured
public final class Match {

	private final String input;
	private final int start;
	private final int end;
	private final int[] slots;

	Match(String input, com.regexpress.matcher.Match found) {
		this.input = input;
		this.start = found.start();
		this.end = found.end();
		this.slots = found.slots().clone();
	}

	public int start() {
		return start;
	}

	public int end() {
		return end;
	}

	// the number of capture groups; group 0, the whole match, is not counted
	public int groupCount() {
		return slots.length / 2;
	}

	// the whole match
	public String group() {
		return group(0);
	}

	// group 0 is the whole match, group i the i-th capturing parenthesis; null when it did not take part
	public String group(int index) {
		if (index == 0) {
			return input.substring(start, end);
		}
		int groupStart = slots[2 * (index - 1)];
		int groupEnd = slots[2 * (index - 1) + 1];
		if (groupStart == -1) {
			return null;
		}
		return input.substring(groupStart, groupEnd);
	}
}
