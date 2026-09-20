package com.regexpress.tokenizer;

public record Literal(char value, int position) implements Token { }