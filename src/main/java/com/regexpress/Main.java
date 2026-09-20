package com.regexpress;

import com.regexpress.ast.AstPrinter;
import com.regexpress.ast.Node;
import com.regexpress.matcher.NfaMatcher;
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
        String pattern = "a{2,}";
        String input = "aa";

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
        } catch (RegexSyntaxException e) {
            System.out.println(e.describe());
        }
    }
}
