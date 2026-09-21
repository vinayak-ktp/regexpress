package com.regexpress.tokenizer;

public sealed interface Token permits Literal, Operator, ClassShorthand, End {
	int position();
}
