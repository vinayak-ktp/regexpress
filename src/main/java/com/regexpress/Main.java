package com.regexpress;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.AstPrinter;
import com.regexpress.ast.CharNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.StarNode;
import com.regexpress.ast.Node;
import com.regexpress.nfa.Nfa;
import com.regexpress.nfa.NfaBuilder;
import com.regexpress.nfa.NfaPrinter;
import com.regexpress.parser.Parser;
import com.regexpress.parser.RegexSyntaxException;

/**
 * Scratch runner for regexpress, a regular expression engine.
 *
 * This is a throwaway harness: as we build each stage of the pipeline
 * (parse -> compile -> match) we wire it up here to see it work end to end.
 */
public final class Main {

    public static void main(String[] args) {

//        Node ast = new ConcatNode(new CharNode('a'), new StarNode(new CharNode('b')));
//        Node ast = new ConcatNode(new StarNode(new AlternateNode(new CharNode('a'), new CharNode('b'))), new CharNode('c'));
//        System.out.println(AstPrinter.flat(ast));
//        AstPrinter.printTree(ast);

//        String pattern = "ab|c)d";
        String pattern = "a|bcd|e*";

        try {
            Node node = Parser.parse(pattern);
            AstPrinter.printFlat(node);
            Nfa machine = NfaBuilder.build(node);
            NfaPrinter.print(machine);
        } catch (RegexSyntaxException e) {
            System.out.println(e.describe());
        }
    }
}
