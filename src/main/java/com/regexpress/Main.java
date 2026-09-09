package com.regexpress;

import com.regexpress.ast.AlternateNode;
import com.regexpress.ast.AstPrinter;
import com.regexpress.ast.CharNode;
import com.regexpress.ast.ConcatNode;
import com.regexpress.ast.StarNode;
import com.regexpress.ast.Node;

/**
 * Scratch runner for regexpress, a regular expression engine.
 *
 * This is a throwaway harness: as we build each stage of the pipeline
 * (parse -> compile -> match) we wire it up here to see it work end to end.
 */
public final class Main {

    public static void main(String[] args) {
//        System.out.println("regexpress skeleton is alive.");
//        System.out.println("Pipeline stages still to build:");
//        System.out.println("  1. parser  : regex text  -> AST");
//        System.out.println("  2. nfa     : AST         -> NFA");
//        System.out.println("  3. matcher : NFA + input -> boolean");

//        Node ast = new ConcatNode(new CharNode('a'), new StarNode(new CharNode('b')));
        Node ast = new ConcatNode(new StarNode(new AlternateNode(new CharNode('a'), new CharNode('b'))), new CharNode('c'));
//        System.out.println(AstPrinter.flat(ast));
        AstPrinter.printTree(ast);
    }


}
