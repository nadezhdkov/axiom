package io.axiom.collections;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based invariants for {@link TreePMap}/{@link TreePSet}: iteration order must always
 * match sorted key/element order, regardless of insertion order — the whole point of the
 * "sorted" family, so axiom.md §11's non-negotiable-invariant bar applies here too.
 */
class TreePMapPropertyTest {

    @Property
    void mapIterationOrderIsSortedRegardlessOfInsertionOrder(@ForAll List<Integer> keys) {
        PSortedMap<Integer, Integer> map = TreePMap.empty();
        for (int k : keys) {
            map = map.plus(k, k);
        }

        List<Integer> expectedSorted = new ArrayList<>(map.keySet());
        expectedSorted.sort(null);

        assertEquals(expectedSorted, List.copyOf(map.keySet()));
    }

    @Property
    void setIterationOrderIsSortedRegardlessOfInsertionOrder(@ForAll List<Integer> values) {
        PSortedSet<Integer> set = TreePSet.empty();
        for (int v : values) {
            set = set.plus(v);
        }

        List<Integer> expectedSorted = new ArrayList<>(set);
        expectedSorted.sort(null);

        assertEquals(expectedSorted, List.copyOf(set));
    }

    @Property
    void mapMutationNeverAffectsThePriorVersion(@ForAll List<Integer> keys) {
        PSortedMap<Integer, Integer> before = TreePMap.empty();
        for (int k : keys) {
            before = before.plus(k, k);
        }

        PSortedMap<Integer, Integer> after = before;
        for (int k : keys) {
            after = after.plus(k, -k);
        }

        for (int k : keys) {
            assertEquals(k, before.get(k));
            assertEquals(-k, after.get(k));
        }
    }

    @Property
    void descendingSetIsExactReverseOfAscendingIteration(@ForAll List<Integer> values) {
        PSortedSet<Integer> set = TreePSet.empty();
        for (int v : values) {
            set = set.plus(v);
        }

        List<Integer> ascending = List.copyOf(set);
        List<Integer> descending = List.copyOf(set.descendingSet());
        List<Integer> reversedAscending = new ArrayList<>(ascending);
        java.util.Collections.reverse(reversedAscending);

        assertEquals(reversedAscending, descending);
    }
}
