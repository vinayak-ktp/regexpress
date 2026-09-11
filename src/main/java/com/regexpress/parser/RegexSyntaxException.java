package com.regexpress.parser;

public final class RegexSyntaxException extends IllegalArgumentException {

	private final String pattern;
	private final int position;

	public RegexSyntaxException(String message, String pattern, int position) {
		super(message);
		this.pattern = pattern;
		this.position = position;
	}

	public String getPattern() {
		return pattern;
	}

	public int getPosition() {
		return position;
	}

	public String describe() {
		return "ERROR: " + getMessage() + "\n\t" + pattern + "\n\t" + " ".repeat(position) + "^";
	}
}
