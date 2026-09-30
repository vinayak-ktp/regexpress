package com.regexpress.tokenizer.tokens;

public sealed interface Token permits Literal, Operator, ClassShorthand, End {
	int position();
}
