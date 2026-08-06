package io.axiom.collections;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreePMapTest {

    @Test
    void emptyMapHasNoEntries() {
        PSortedMap<Integer, String> m = TreePMap.empty();
        assertEquals(0, m.size());
        assertTrue(m.isEmpty());
    }

    @Test
    void plusAddsEntryWithoutMutatingOriginal() {
        PSortedMap<Integer, String> m0 = TreePMap.empty();
        PSortedMap<Integer, String> m1 = m0.plus(2, "B");

        assertEquals(0, m0.size());
        assertEquals("B", m1.get(2));
    }

    @Test
    void plusWithSameKeyValueReturnsSameInstance() {
        PSortedMap<Integer, String> m1 = TreePMap.<Integer, String>empty().plus(1, "A");
        PSortedMap<Integer, String> m2 = m1.plus(1, "A");
        assertSame(m1, m2);
    }

    @Test
    void iterationFollowsKeyOrder() {
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty()
                .plus(3, "C").plus(1, "A").plus(2, "B");

        assertEquals(java.util.List.of(1, 2, 3), java.util.List.copyOf(m.keySet()));
    }

    @Test
    void navigationMethodsFollowSortedOrder() {
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty()
                .plus(1, "A").plus(2, "B").plus(3, "C");

        assertEquals(1, m.firstKey());
        assertEquals(3, m.lastKey());
        assertEquals(2, m.floorKey(2));
        assertEquals(2, m.ceilingKey(2));
        assertEquals(1, m.lowerKey(2));
        assertEquals(3, m.higherKey(2));
    }

    @Test
    void descendingMapReversesIterationOrder() {
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty()
                .plus(1, "A").plus(2, "B").plus(3, "C");

        assertEquals(java.util.List.of(3, 2, 1), java.util.List.copyOf(m.descendingMap().keySet()));
    }

    @Test
    void navigableKeySetIsAPersistentSortedSet() {
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty().plus(2, "B").plus(1, "A");
        PSortedSet<Integer> keys = m.navigableKeySet();

        assertEquals(java.util.List.of(1, 2), java.util.List.copyOf(keys));
    }

    @Test
    void customComparatorControlsOrdering() {
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty(Comparator.reverseOrder())
                .plus(1, "A").plus(2, "B").plus(3, "C");

        assertEquals(java.util.List.of(3, 2, 1), java.util.List.copyOf(m.keySet()));
    }

    @Test
    void minusRemovesKeyWithoutAffectingOriginal() {
        PSortedMap<Integer, String> m1 = TreePMap.<Integer, String>empty().plus(1, "A").plus(2, "B");
        PSortedMap<Integer, String> m2 = m1.minus(1);

        assertTrue(m1.containsKey(1));
        assertNull(m2.get(1));
    }

    @Test
    void deprecatedTwoArgSubMapDelegatesInsteadOfReturningNull() {
        // Regression guard for a bug in the reference this was ported from: the deprecated
        // 2-arg subMap/headMap/tailMap overloads there returned null unconditionally.
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty()
                .plus(1, "A").plus(2, "B").plus(3, "C");

        assertEquals(java.util.List.of(1, 2), java.util.List.copyOf(m.subMap(1, 3).keySet()));
        assertEquals(java.util.List.of(1, 2), java.util.List.copyOf(m.headMap(3).keySet()));
        assertEquals(java.util.List.of(2, 3), java.util.List.copyOf(m.tailMap(2).keySet()));
    }

    @Test
    void mutatingMapMethodsThrow() {
        PSortedMap<Integer, String> m = TreePMap.<Integer, String>empty().plus(1, "A");
        assertThrows(UnsupportedOperationException.class, () -> m.put(2, "B"));
        assertThrows(UnsupportedOperationException.class, () -> m.remove(1));
        assertThrows(UnsupportedOperationException.class, () -> m.putAll(Map.of()));
        assertThrows(UnsupportedOperationException.class, m::clear);
    }
}
