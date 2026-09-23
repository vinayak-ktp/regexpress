package com.regexpress;

import com.regexpress.ast.AstPrinter;
import com.regexpress.ast.Node;
import com.regexpress.matcher.Match;
import com.regexpress.matcher.NfaMatcher;
import com.regexpress.matcher.PikeMatcher;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.nfa.NfaPrinter;
import com.regexpress.parser.Parser;
import com.regexpress.tokenizer.RegexSyntaxException;

public final class Main {

    public static void main(String[] args) {

//        Node ast = new ConcatNode(new CharNode('a'), new StarNode(new CharNode('b')));
//        Node ast = new ConcatNode(new StarNode(new AlternateNode(new CharNode('a'), new CharNode('b'))), new CharNode('c'));
//        System.out.println(AstPrinter.flat(ast));
//        AstPrinter.printTree(ast);

//        String pattern = "ab|c)d";
//        String pattern = "a|b?(cd+|e)*.";
//        String pattern = "a[x-zt-vf-]c.";
//        String pattern = "^a{2,}b$";
        String pattern = "a (cat|dog)";
        String input = "there's a cat in my yard, but I kinda wish it was a dog. I used to own a dog.";

        try {
            Node node = Parser.parse(pattern);
            System.out.println("AST for " + pattern + ": ");
            AstPrinter.printFlat(node);
//            AstPrinter.printTree(node);

            Nfa machine = NfaBuilder.build(node);
            System.out.println("\nNFA for " + pattern + ": ");
            NfaPrinter.print(machine);

            boolean matches = NfaMatcher.matches(machine, input);
            String verdict = matches ? " matches " : " does not match ";
            System.out.println("\ninput \"" + input + "\"" + verdict + "the pattern \"" + pattern + "\"");

            boolean any = false;
            int from = 0;
            while (from <= input.length()) {
                Match match = PikeMatcher.find(machine, input, from);
                if (match == null) {
                    break;
                }
                any = true;

                System.out.println("found \"" + input.substring(match.start(), match.end())
                    + "\" at " + match.start() + ".." + match.end());

                for (int g = 1; g <= machine.groupCount; g++) {
                    int groupStart = match.slots()[2 * (g - 1)];
                    int groupEnd = match.slots()[2 * (g - 1) + 1];
                    if (groupStart == -1) {
                        System.out.println("group " + g + ": did not take part");
                    } else {
                        System.out.println("group " + g + ": \"" + input.substring(groupStart, groupEnd)
                            + "\" (" + groupStart + ".." + groupEnd + ")");
                    }
                }

                // move past this match; if it was empty, step one further so the loop can't get stuck
                from = match.end() == match.start() ? match.end() + 1 : match.end();
            }

            if (!any) {
                System.out.println("no match found");
            }
        } catch (RegexSyntaxException e) {
            System.out.println(e.describe());
        }
    }
}
