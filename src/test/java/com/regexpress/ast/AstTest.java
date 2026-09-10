package com.regexpress.ast;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

public class AstTest {
	public static void main(String[] args) {
		// "a(b|c)*"
		Node treeA = new ConcatNode(new CharNode('a'), new StarNode(new AlternateNode(new CharNode('b'), new CharNode('c'))));
		// "a(b|c)*
		Node treeB = new ConcatNode(new CharNode('a'), new StarNode(new AlternateNode(new CharNode('b'), new CharNode('c'))));
		// "a(b*|c)"
		Node treeC = new ConcatNode(new CharNode('a'), new AlternateNode(new StarNode(new CharNode('b')), new CharNode('c')));

		Node emptyNodeA = new EmptyNode();
		Node emptyNodeB = new EmptyNode();

		CharNode charNode = new CharNode('a');

		// check tree equality
		check("two independently built trees for the same pattern compare as equal", treeA, treeB);
		check("two trees with different structure do not compare as equal", treeA, treeC, false);

		check("two independently created EmptyNode instances compare as equal", emptyNodeA, emptyNodeB);
		check("CharNode.value() returns the character it was constructed with", 'a', charNode.value());

		// check passing null
		checkThrows("AlternateNode rejects a null left child", NullPointerException.class, () -> new AlternateNode(null, new EmptyNode()));
		checkThrows("AlternateNode rejects a null right child", NullPointerException.class, () -> new AlternateNode(new EmptyNode(), null));
		checkThrows("ConcatNode rejects a null left child", NullPointerException.class, () -> new ConcatNode(null, new EmptyNode()));
		checkThrows("ConcatNode rejects a null right child", NullPointerException.class, () -> new ConcatNode(new EmptyNode(), null));
		checkThrows("StarNode rejects a null child", NullPointerException.class, () -> new StarNode(null));

		report();
	}
}
