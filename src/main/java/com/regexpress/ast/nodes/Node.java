package com.regexpress.ast.nodes;

public sealed interface Node permits CharSetNode, ConcatNode, AlternateNode, StarNode, EmptyNode,
		PlusNode, OptionalNode, StartAnchorNode, EndAnchorNode, GroupNode { }