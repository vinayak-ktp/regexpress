package com.regexpress;

import java.util.Objects;

public final class TestSupport {

	private static int passed;
	private static int failed;

	public static void check(String label, Object expected, Object actual) {
		check(label, expected, actual, true);
	}

	public static void check(String label, Object expected, Object actual, boolean shouldMatch) {
		if (Objects.equals(expected, actual) == shouldMatch) {
			passed++;
		} else {
			String expectedLabel = shouldMatch ? "Expected" : "Expected NOT";
			System.out.println("FAIL: " + label + "\n\t" + expectedLabel + ": " + (expected == null ? "null" : expected.toString()) +
					"\n\tActual: " + (actual == null ? "null" : actual.toString()) + "\n");
			failed++;
		}
	}

	public static void checkThrows(String label, Class<? extends Exception> expectedType, Runnable action) {
		try {
			action.run();
			failed++;
			System.out.println("FAIL: " + label + "\n\texpected " + expectedType.getSimpleName() + " but nothing was thrown\n");
		} catch (Exception e) {
			if (expectedType.isInstance(e)) {
				passed++;
			} else {
				failed++;
				System.out.println("FAIL: " + label + "\n\texpected: " + expectedType.getSimpleName() +
						"\n\tactual: " + e.getClass().getSimpleName() + "\n");
			}
		}
	}

	public static void report() {
		System.out.println(getPassed() + " passed, " + getFailed() + " failed\n");
	}

	public static int getPassed() {
		return passed;
	}

	public static int getFailed() {
		return failed;
	}
}
