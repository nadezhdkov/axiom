package io.axiom.collections;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreePSetTest {

    @Test
    void emptySetHasNoElements() {
        PSortedSet<Integer> s = TreePSet.empty();
        assertEquals(0, s.size());
        assertTrue(s.isEmpty());
    }

    @Test
    void plusAddsElementWithoutMutatingOriginal() {
        PSortedSet<Integer> s0 = TreePSet.empty();
        PSortedSet<Integer> s1 = s0.plus(2).plus(1).plus(3);

        assertEquals(0, s0.size());
        assertEquals(List.of(1, 2, 3), List.copyOf(s1));
    }

    @Test
    void plusOfExistingElementReturnsThis() {
        PSortedSet<Integer> s1 = TreePSet.<Integer>empty().plus(1);
        PSortedSet<Integer> s2 = s1.plus(1);
        assertEquals(s1, s2);
    }

    @Test
    void iterationFollowsSortedOrder() {
        PSortedSet<Integer> s = TreePSet.<Integer>empty().plus(3).plus(1).plus(2);
        assertEquals(List.of(1, 2, 3), List.copyOf(s));
    }

    @Test
    void navigationMethodsFollowSortedOrder() {
        PSortedSet<Integer> s = TreePSet.<Integer>empty().plus(1).plus(2).plus(3);
        assertEquals(1, s.first());
        assertEquals(3, s.last());
        assertEquals(2, s.floor(2));
        assertEquals(2, s.ceiling(2));
        assertEquals(1, s.lower(2));
        assertEquals(3, s.higher(2));
    }

    @Test
    void descendingSetReversesIterationOrder() {
        PSortedSet<Integer> s = TreePSet.<Integer>empty().plus(1).plus(2).plus(3);
        assertEquals(List.of(3, 2, 1), List.copyOf(s.descendingSet()));
    }

    @Test
    void descendingIteratorActuallyIteratesInsteadOfNpeing() {
        // Regression guard for a bug in the reference this was ported from: descendingIterator()
        // there was annotated @Deprecated and returned null unconditionally.
        PSortedSet<Integer> s = TreePSet.<Integer>empty().plus(1).plus(2).plus(3);
        Iterator<Integer> it = s.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
    }

    @Test
    void deprecatedTwoArgSubSetDelegatesInsteadOfReturningNull() {
        PSortedSet<Integer> s = TreePSet.<Integer>empty().plus(1).plus(2).plus(3);
        assertEquals(List.of(1, 2), List.copyOf(s.subSet(1, 3)));
        assertEquals(List.of(1, 2), List.copyOf(s.headSet(3)));
        assertEquals(List.of(2, 3), List.copyOf(s.tailSet(2)));
    }

    @Test
    void customComparatorControlsOrdering() {
        PSortedSet<Integer> s = TreePSet.<Integer>empty(Comparator.reverseOrder()).plus(1).plus(2).plus(3);
        assertEquals(List.of(3, 2, 1), List.copyOf(s));
    }

    @Test
    void minusRemovesElementWithoutAffectingOriginal() {
        PSortedSet<Integer> s1 = TreePSet.<Integer>empty().plus(1).plus(2);
        PSortedSet<Integer> s2 = s1.minus(1);

        assertTrue(s1.contains(1));
        assertFalse(s2.contains(1));
    }

    @Test
    void mutatingSetMethodsThrow() {
        PSortedSet<Integer> s = TreePSet.<Integer>empty().plus(1);
        assertThrows(UnsupportedOperationException.class, () -> s.add(2));
        assertThrows(UnsupportedOperationException.class, () -> s.remove(1));
        assertThrows(UnsupportedOperationException.class, s::clear);
        assertThrows(UnsupportedOperationException.class, s::pollFirst);
    }
}
