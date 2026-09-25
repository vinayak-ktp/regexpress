package com.regexpress.ast;

import static com.regexpress.TestSupport.check;
import static com.regexpress.TestSupport.checkThrows;
import static com.regexpress.TestSupport.report;

public class AstTest {
	public static void main(String[] args) {
		// "a(b|c)*"
		Node treeA = new ConcatNode(new CharSetNode(CharSet.of('a')), new StarNode(new AlternateNode(new CharSetNode(CharSet.of('b')), new CharSetNode(CharSet.of('c'))), false));
		// "a(b|c)*
		Node treeB = new ConcatNode(new CharSetNode(CharSet.of('a')), new StarNode(new AlternateNode(new CharSetNode(CharSet.of('b')), new CharSetNode(CharSet.of('c'))), false));
		// "a(b*|c)"
		Node treeC = new ConcatNode(new CharSetNode(CharSet.of('a')), new AlternateNode(new StarNode(new CharSetNode(CharSet.of('b')), false), new CharSetNode(CharSet.of('c'))));

		Node emptyNodeA = new EmptyNode();
		Node emptyNodeB = new EmptyNode();

		CharSetNode charNode = new CharSetNode(CharSet.of('a'));

		// check tree equality
		check("two independently built trees for the same pattern compare as equal", treeA, treeB);
		check("two trees with different structure do not compare as equal", treeA, treeC, false);

		check("two independently created EmptyNode instances compare as equal", emptyNodeA, emptyNodeB);
		check("CharSetNode.set() returns the CharSet it was constructed with", CharSet.of('a'), charNode.set());

		// check passing null
		checkThrows("AlternateNode rejects a null left child", NullPointerException.class, () -> new AlternateNode(null, new EmptyNode()));
		checkThrows("AlternateNode rejects a null right child", NullPointerException.class, () -> new AlternateNode(new EmptyNode(), null));
		checkThrows("ConcatNode rejects a null left child", NullPointerException.class, () -> new ConcatNode(null, new EmptyNode()));
		checkThrows("ConcatNode rejects a null right child", NullPointerException.class, () -> new ConcatNode(new EmptyNode(), null));
		checkThrows("StarNode rejects a null child", NullPointerException.class, () -> new StarNode(null, false));
		checkThrows("PlusNode rejects a null child", NullPointerException.class, () -> new PlusNode(null, false));
		checkThrows("OptionalNode rejects a null child", NullPointerException.class, () -> new OptionalNode(null, false));

		report();
	}
}
