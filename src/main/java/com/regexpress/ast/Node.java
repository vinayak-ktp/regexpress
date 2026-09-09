package com.regexpress.ast;

public sealed interface Node permits CharNode, ConcatNode, AlternateNode, StarNode, EmptyNode { }