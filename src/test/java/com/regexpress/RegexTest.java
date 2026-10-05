package com.regexpress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.regexpress.tokenizer.RegexSyntaxException;

class RegexTest {

	static Stream<Arguments> compileCases() {
		return Stream.of(
				Arguments.of("a+", "aaa"), Arguments.of("a+", ""), Arguments.of("ab*", "abb"),
				Arguments.of("(a|b)*", "abba"), Arguments.of("[a-c]{2,}", "cab"), Arguments.of("\\d+", "123"),
				Arguments.of("^a$", "a"), Arguments.of("a$b", "ab"));
	}

	@Nested
	@DisplayName("compiling once and reusing")
	class CompilingOnceAndReusing {

		@ParameterizedTest(name = "a compiled pattern gives the same answer when reused: \"{0}\" against \"{1}\"")
		@MethodSource("com.regexpress.RegexTest#compileCases")
		void compiledPatternGivesSameAnswerWhenReused(String pattern, String input) {
			Regex compiled = Regex.compile(pattern);
			assertEquals(compiled.matches(input), compiled.matches(input));
		}

		@ParameterizedTest(name = "the one-off static agrees with the compiled object: \"{0}\" against \"{1}\"")
		@MethodSource("com.regexpress.RegexTest#compileCases")
		void oneOffStaticAgreesWithCompiledObject(String pattern, String input) {
			boolean first = Regex.compile(pattern).matches(input);
			assertEquals(first, Regex.matches(pattern, input));
		}

		@ParameterizedTest(name = "java.util.regex agrees with the facade: \"{0}\" against \"{1}\"")
		@MethodSource("com.regexpress.RegexTest#compileCases")
		void javaUtilRegexAgreesWithFacade(String pattern, String input) {
			boolean first = Regex.compile(pattern).matches(input);
			assertEquals(Pattern.matches(pattern, input), first);
		}
	}

	@Nested
	@DisplayName("rejections")
	class Rejections {
		@Test
		@DisplayName("compiling an invalid pattern throws the syntax exception")
		void compilingInvalidPatternThrows() {
			assertThrows(RegexSyntaxException.class, () -> Regex.compile("a("));
		}

		@Test
		@DisplayName("the one-off static rejects an invalid pattern too")
		void oneOffStaticRejectsInvalidPatternToo() {
			assertThrows(RegexSyntaxException.class, () -> Regex.matches("a(", "a"));
		}
	}

	@Nested
	@DisplayName("find and groups through the facade")
	class FindAndGroupsThroughFacade {
		Match petsMatch = Regex.compile("a (cat|dog)").find("there's a cat in my yard");

		@Test
		@DisplayName("find reports where the match starts")
		void findReportsWhereMatchStarts() {
			assertEquals(8, petsMatch.start());
		}

		@Test
		@DisplayName("find reports where the match ends")
		void findReportsWhereMatchEnds() {
			assertEquals(13, petsMatch.end());
		}

		@Test
		@DisplayName("group zero is the whole match")
		void groupZeroIsWholeMatch() {
			assertEquals("a cat", petsMatch.group(0));
		}

		@Test
		@DisplayName("group one is the first capturing parenthesis")
		void groupOneIsFirstCapturingParenthesis() {
			assertEquals("cat", petsMatch.group(1));
		}

		@Test
		@DisplayName("group with no braces around it gives the whole match")
		void groupWithNoBracesGivesWholeMatch() {
			assertEquals("a cat", petsMatch.group());
		}

		@Test
		@DisplayName("find with no match gives null")
		void findWithNoMatchGivesNull() {
			assertNull(Regex.compile("x").find("abc"));
		}

		@Test
		@DisplayName("a skipped optional group reads as null")
		void skippedOptionalGroupReadsAsNull() {
			assertNull(Regex.compile("(a)?b").find("b").group(1));
		}

		@Test
		@DisplayName("find resumes from the given position")
		void findResumesFromGivenPosition() {
			assertEquals(10, Regex.compile("a (cat|dog)").find("a cat and a dog", 2).start());
		}
	}

	@Nested
	@DisplayName("replaceAll and split through the facade")
	class ReplaceAllAndSplitThroughFacade {
		@Test
		@DisplayName("replaceAll through the facade")
		void replaceAllThroughFacade() {
			assertEquals("the X and the X", Regex.compile("cat|dog").replaceAll("the cat and the dog", "X"));
		}

		@Test
		@DisplayName("split through the facade")
		void splitThroughFacade() {
			assertEquals(List.of("a", "b", "c"), Regex.compile("\\d+").split("a1b22c"));
		}
	}

	// the facade against java.util.regex on spans and every group
	static final String[][] FIND_CASES = {
			{ "a (cat|dog)", "there's a cat in my yard" },
			{ "(a|ab)(c|bcd)", "abcd" },
			{ "(a)?b", "b" },
			{ "(a)(b)", "abab" },
			{ "<.*?>", "<b>bold</b> and <i>italic</i>" },
			{ "x", "abc" }
	};

	static Stream<Arguments> findCases() {
		return Stream.of(FIND_CASES).map(c -> Arguments.of(c[0], c[1]));
	}

	// only the cases where java.util.regex actually finds a match, since only those have a span/groups to compare
	static Stream<Arguments> findCasesThatMatch() {
		return Stream.of(FIND_CASES)
				.filter(c -> Pattern.compile(c[0]).matcher(c[1]).find())
				.map(c -> Arguments.of(c[0], c[1]));
	}

	// (pattern, input, group index) for every group, including group 0, of every case that matches
	static Stream<Arguments> findCasesGroupIndexes() {
		return Stream.of(FIND_CASES)
				.<Arguments>mapMulti((c, consumer) -> {
					java.util.regex.Matcher m = Pattern.compile(c[0]).matcher(c[1]);
					if (!m.find()) {
						return;
					}
					for (int g = 0; g <= m.groupCount(); g++) {
						consumer.accept(Arguments.of(c[0], c[1], g));
					}
				});
	}

	@ParameterizedTest(name = "the facade find agrees with java.util.regex on finding at all: \"{0}\" against \"{1}\"")
	@MethodSource("findCases")
	void facadeFindAgreesOnFindingAtAll(String pattern, String input) {
		boolean found = Pattern.compile(pattern).matcher(input).find();
		Match ours = Regex.compile(pattern).find(input);
		assertEquals(found, ours != null);
	}

	@ParameterizedTest(name = "the facade find agrees with java.util.regex on the span: \"{0}\" against \"{1}\"")
	@MethodSource("findCasesThatMatch")
	void facadeFindAgreesOnSpan(String pattern, String input) {
		java.util.regex.Matcher java = Pattern.compile(pattern).matcher(input);
		java.find();
		Match ours = Regex.compile(pattern).find(input);
		assertEquals(java.start() + ".." + java.end(), ours.start() + ".." + ours.end());
	}

	@ParameterizedTest(name = "the facade group {2} agrees with java.util.regex: \"{0}\" against \"{1}\"")
	@MethodSource("findCasesGroupIndexes")
	void facadeGroupAgreesWithJavaUtilRegex(String pattern, String input, int groupIndex) {
		java.util.regex.Matcher java = Pattern.compile(pattern).matcher(input);
		java.find();
		Match ours = Regex.compile(pattern).find(input);
		assertEquals(java.group(groupIndex), ours.group(groupIndex));
	}
}
