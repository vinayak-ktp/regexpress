package com.regexpress;

import com.regexpress.tokenizer.RegexSyntaxException;

public final class Main {

    public static void main(String[] args) {
        String pattern = "a (cat|dog)";
        String input = "there's a cat in my yard, but I kinda wish it was a dog. I used to own a dog.";

        try {
            // compile once and reuse
            Regex regex = Regex.compile(pattern);

            System.out.println("pattern:    " + pattern);
            System.out.println("input:      " + input);
            System.out.println("full match: " + regex.matches(input));
            System.out.println();

            // the leftmost match, then every later one
            Match match = regex.find(input);
            while (match != null) {
                System.out.println("found \"" + match.group() + "\" at " + match.start() + ".." + match.end());
                for (int g = 1; g <= match.groupCount(); g++) {
                    System.out.println("  group " + g + ": " + (match.group(g) == null ? "did not take part" : "\"" + match.group(g) + "\""));
                }

                // resume after this match; an empty match needs one extra step so the loop cannot get stuck
                int from = match.end() == match.start() ? match.end() + 1 : match.end();
                match = regex.find(input, from);
            }
            System.out.println();

            // text operations built on find
            System.out.println("replaceAll: " + regex.replaceAll(input, "pet"));
            System.out.println("split: " + Regex.compile("\\d+").split("a1b22c333d"));

            // one-off convenience, for code that runs only once
            System.out.println("one-off: " + Regex.matches("a+", "aaa"));
        } catch (RegexSyntaxException e) {
            System.out.println(e.describe());
        }
    }
}
