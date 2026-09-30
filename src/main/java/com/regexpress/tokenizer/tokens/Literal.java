package com.regexpress.tokenizer.tokens;

public record Literal(char value, int position) implements Token { }