package com.regexpress.tokenizer;

public sealed interface Token permits Literal, Operator, End {
	int position();
}
