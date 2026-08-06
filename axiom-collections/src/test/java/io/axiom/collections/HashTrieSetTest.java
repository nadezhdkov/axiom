package io.axiom.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashTrieSetTest {

    @Test
    void emptySetHasNoElements() {
        assertEquals(0, HashTrieSet.empty().size());
    }

    @Test
    void plusAddsElementWithoutMutatingOriginal() {
        HashTrieSet<String> s0 = HashTrieSet.empty();
        HashTrieSet<String> s1 = s0.plus("a");

        assertFalse(s0.contains("a"));
        assertTrue(s1.contains("a"));
        assertEquals(1, s1.size());
    }

    @Test
    void duplicateInsertionReturnsSameInstance() {
        HashTrieSet<String> s1 = HashTrieSet.<String>empty().plus("a");
        HashTrieSet<String> s2 = s1.plus("a");

        assertSame(s1, s2);
        assertEquals(1, s2.size());
    }

    @Test
    void minusRemovesElementWithoutMutatingOriginal() {
        HashTrieSet<String> s1 = HashTrieSet.<String>empty().plus("a").plus("b");
        HashTrieSet<String> s2 = s1.minus("a");

        assertEquals(2, s1.size());
        assertTrue(s1.contains("a"));
        assertEquals(1, s2.size());
        assertFalse(s2.contains("a"));
    }

    @Test
    void plusAllAndMinusAll() {
        HashTrieSet<Integer> s = HashTrieSet.<Integer>empty().plusAll(java.util.List.of(1, 2, 3));
        assertEquals(3, s.size());

        HashTrieSet<Integer> reduced = s.minusAll(java.util.List.of(1, 2));
        assertEquals(1, reduced.size());
        assertTrue(reduced.contains(3));
    }
}
