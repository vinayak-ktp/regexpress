package com.regexpress;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

import java.util.List;
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

		// check find and groups through the facade
		Match petsMatch = Regex.compile("a (cat|dog)").find("there's a cat in my yard");
		check("find reports where the match starts", 8, petsMatch.start());
		check("find reports where the match ends", 13, petsMatch.end());
		check("group zero is the whole match", "a cat", petsMatch.group(0));
		check("group one is the first capturing parenthesis", "cat", petsMatch.group(1));
		check("group with no braces around it gives the whole match", "a cat", petsMatch.group());
		check("find with no match gives null", null, Regex.compile("x").find("abc"));
		check("a skipped optional group reads as null", null, Regex.compile("(a)?b").find("b").group(1));
		check("find resumes from the given position", 10, Regex.compile("a (cat|dog)").find("a cat and a dog", 2).start());

		// check replaceAll and split through the facade
		check("replaceAll through the facade", "the X and the X",
			Regex.compile("cat|dog").replaceAll("the cat and the dog", "X"));
		check("split through the facade", List.of("a", "b", "c"), Regex.compile("\\d+").split("a1b22c"));

		// check the facade against java.util.regex on spans and every group
		String[][] findCases = {
			{ "a (cat|dog)", "there's a cat in my yard" },
			{ "(a|ab)(c|bcd)", "abcd" },
			{ "(a)?b", "b" },
			{ "(a)(b)", "abab" },
			{ "<.*?>", "<b>bold</b> and <i>italic</i>" },
			{ "x", "abc" }
		};

		for (String[] c : findCases) {
			Regex compiled = Regex.compile(c[0]);
			Match ours = compiled.find(c[1]);
			java.util.regex.Matcher java = Pattern.compile(c[0]).matcher(c[1]);
			boolean found = java.find();

			check("the facade find agrees with java.util.regex on finding at all", found, ours != null);
			if (ours != null) {
				check("the facade find agrees with java.util.regex on the span",
					java.start() + ".." + java.end(), ours.start() + ".." + ours.end());
				for (int g = 0; g <= java.groupCount(); g++) {
					check("the facade group " + g + " agrees with java.util.regex", java.group(g), ours.group(g));
				}
			}
		}

		report();
	}
}
