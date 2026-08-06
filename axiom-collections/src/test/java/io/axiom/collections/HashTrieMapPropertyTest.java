package io.axiom.collections;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based tests for {@link HashTrieMap}'s HAMT invariants — axiom.md §11 treats this
 * as a non-negotiable requirement, not optional coverage: path copying must never affect a
 * prior version, structural equality must hold regardless of insertion order, and behavior
 * under hash collisions must remain correct.
 */
class HashTrieMapPropertyTest {

    @Property
    void pathCopyingNeverAffectsThePriorVersion(@ForAll("distinctKeys") List<Integer> keys) {
        HashTrieMap<Integer, Integer> before = HashTrieMap.empty();
        for (int k : keys) {
            before = before.plus(k, k);
        }

        HashTrieMap<Integer, Integer> after = before;
        for (int k : keys) {
            after = after.plus(k, -k);
        }

        // "before" must remain exactly as it was, unaffected by mutations derived from it
        for (int k : keys) {
            assertEquals(k, before.get(k));
            assertEquals(-k, after.get(k));
        }
        assertEquals(keys.size(), before.size());
    }

    @Property
    void removingAKeyNeverAffectsThePriorVersion(@ForAll("distinctKeys") List<Integer> keys) {
        if (keys.isEmpty()) return;

        HashTrieMap<Integer, Integer> full = HashTrieMap.empty();
        for (int k : keys) {
            full = full.plus(k, k);
        }

        int removedKey = keys.get(0);
        HashTrieMap<Integer, Integer> reduced = full.minus(removedKey);

        assertTrue(full.containsKey(removedKey));
        assertEquals(keys.size(), full.size());

        assertEquals(keys.size() - 1, reduced.size());
        assertNull(reduced.get(removedKey));
    }

    @Property
    void structuralEqualityIsIndependentOfInsertionOrder(
            @ForAll("distinctKeys") List<Integer> keys,
            @ForAll long shuffleSeed
    ) {
        HashTrieMap<Integer, Integer> forward = HashTrieMap.empty();
        for (int k : keys) forward = forward.plus(k, k);

        List<Integer> shuffled = new java.util.ArrayList<>(keys);
        java.util.Collections.shuffle(shuffled, new java.util.Random(shuffleSeed));

        HashTrieMap<Integer, Integer> backward = HashTrieMap.empty();
        for (int k : shuffled) backward = backward.plus(k, k);

        assertEquals(forward, backward);
        assertEquals(forward.hashCode(), backward.hashCode());
    }

    @Property
    void allInsertedKeysAreRetrievableEvenUnderForcedHashCollisions(
            @ForAll("distinctKeys") List<Integer> keys,
            @ForAll("smallHashSpace") int hashBucketCount
    ) {
        // Force every key into a small number of hash buckets, guaranteeing CollisionNode usage.
        HashTrieMap<CollidingInt, Integer> m = HashTrieMap.empty();
        for (int k : keys) {
            m = m.plus(new CollidingInt(k, k % hashBucketCount), k);
        }

        assertEquals(keys.size(), m.size());
        for (int k : keys) {
            assertEquals(k, m.get(new CollidingInt(k, k % hashBucketCount)));
        }
    }

    private record CollidingInt(int value, int forcedHash) {
        @Override
        public int hashCode() {
            return forcedHash;
        }
    }

    @net.jqwik.api.Provide("distinctKeys")
    net.jqwik.api.Arbitrary<List<Integer>> distinctKeys() {
        return Arbitraries.integers().between(-2000, 2000).list().uniqueElements().ofMaxSize(200);
    }

    @net.jqwik.api.Provide("smallHashSpace")
    net.jqwik.api.Arbitrary<Integer> smallHashSpace() {
        return Arbitraries.integers().between(1, 8);
    }

    // Sanity check that Map#equals from AbstractMap is actually exercised as expected.
    @net.jqwik.api.Example
    void emptyMapsAreEqual() {
        assertEquals(Map.of(), HashTrieMap.empty());
    }
}
