package com.regexpress.matcher;

// a strategy for answering the yes/no matching question for one compiled machine
public interface Matcher {
	boolean matches(String input);
}
