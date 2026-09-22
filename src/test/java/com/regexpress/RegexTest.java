package com.regexpress;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

import java.util.regex.Pattern;

import com.regexpress.tokenizer.RegexSyntaxException;

public class RegexTest {
	public static void main(String[] args) {
		String[][] cases = {
				{ "a+", "aaa" }, { "a+", "" }, { "ab*", "abb" }, { "(a|b)*", "abba" },
				{ "[a-c]{2,}", "cab" }, { "\\d+", "123" }, { "^a$", "a" }, { "a$b", "ab" }
		};

		// check compiling once and reusing
		for (String[] c : cases) {
			Regex compiled = Regex.compile(c[0]);
			boolean first = compiled.matches(c[1]);
			boolean second = compiled.matches(c[1]);
			check("a compiled pattern gives the same answer when reused", first, second);
			check("the one-off static agrees with the compiled object", first, Regex.matches(c[0], c[1]));
			check("java.util.regex agrees with the facade", Pattern.matches(c[0], c[1]), first);
		}

		// check rejections
		checkThrows("compiling an invalid pattern throws the syntax exception", RegexSyntaxException.class, () -> Regex.compile("a("));
		checkThrows("the one-off static rejects an invalid pattern too", RegexSyntaxException.class, () -> Regex.matches("a(", "a"));

		report();
	}
}
