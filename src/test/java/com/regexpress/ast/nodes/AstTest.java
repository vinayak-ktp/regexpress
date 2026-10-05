package com.regexpress.ast.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.regexpress.ast.CharSet;

class AstTest {

	@Nested
	@DisplayName("tree equality")
	class TreeEquality {

		// "a(b|c)*"
		Node treeA = new ConcatNode(new CharSetNode(CharSet.of('a')), new StarNode(new AlternateNode(new CharSetNode(CharSet.of('b')), new CharSetNode(CharSet.of('c'))), false));
		// "a(b|c)*"
		Node treeB = new ConcatNode(new CharSetNode(CharSet.of('a')), new StarNode(new AlternateNode(new CharSetNode(CharSet.of('b')), new CharSetNode(CharSet.of('c'))), false));
		// "a(b*|c)"
		Node treeC = new ConcatNode(new CharSetNode(CharSet.of('a')), new AlternateNode(new StarNode(new CharSetNode(CharSet.of('b')), false), new CharSetNode(CharSet.of('c'))));

		@Test
		@DisplayName("two independently built trees for the same pattern compare as equal")
		void equalTreesCompareEqual() {
			assertEquals(treeA, treeB);
		}

		@Test
		@DisplayName("two trees with different structure do not compare as equal")
		void differentTreesCompareUnequal() {
			assertNotEquals(treeA, treeC);
		}

		@Test
		@DisplayName("two independently created EmptyNode instances compare as equal")
		void emptyNodesCompareEqual() {
			assertEquals(new EmptyNode(), new EmptyNode());
		}

		@Test
		@DisplayName("CharSetNode.set() returns the CharSet it was constructed with")
		void charSetNodeReturnsItsSet() {
			CharSetNode charNode = new CharSetNode(CharSet.of('a'));
			assertEquals(CharSet.of('a'), charNode.set());
		}
	}

	@Nested
	@DisplayName("passing null")
	class PassingNull {

		@Test
		@DisplayName("AlternateNode rejects a null left child")
		void alternateRejectsNullLeft() {
			assertThrows(NullPointerException.class, () -> new AlternateNode(null, new EmptyNode()));
		}

		@Test
		@DisplayName("AlternateNode rejects a null right child")
		void alternateRejectsNullRight() {
			assertThrows(NullPointerException.class, () -> new AlternateNode(new EmptyNode(), null));
		}

		@Test
		@DisplayName("ConcatNode rejects a null left child")
		void concatRejectsNullLeft() {
			assertThrows(NullPointerException.class, () -> new ConcatNode(null, new EmptyNode()));
		}

		@Test
		@DisplayName("ConcatNode rejects a null right child")
		void concatRejectsNullRight() {
			assertThrows(NullPointerException.class, () -> new ConcatNode(new EmptyNode(), null));
		}

		@Test
		@DisplayName("StarNode rejects a null child")
		void starRejectsNullChild() {
			assertThrows(NullPointerException.class, () -> new StarNode(null, false));
		}

		@Test
		@DisplayName("PlusNode rejects a null child")
		void plusRejectsNullChild() {
			assertThrows(NullPointerException.class, () -> new PlusNode(null, false));
		}

		@Test
		@DisplayName("OptionalNode rejects a null child")
		void optionalRejectsNullChild() {
			assertThrows(NullPointerException.class, () -> new OptionalNode(null, false));
		}
	}
}
