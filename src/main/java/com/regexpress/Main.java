package com.regexpress;

/**
 * Scratch runner for regexpress, a regular expression engine.
 *
 * This is a throwaway harness: as we build each stage of the pipeline
 * (parse -> compile -> match) we wire it up here to see it work end to end.
 */
public final class Main {

    public static void main(String[] args) {
        System.out.println("regexpress skeleton is alive.");
        System.out.println("Pipeline stages still to build:");
        System.out.println("  1. parser  : regex text  -> AST");
        System.out.println("  2. nfa     : AST         -> NFA");
        System.out.println("  3. matcher : NFA + input -> boolean");
    }
}
