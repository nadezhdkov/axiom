package io.axiom.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashTrieMapTest {

    @Test
    void emptyMapHasNoEntries() {
        HashTrieMap<String, Integer> m = HashTrieMap.empty();
        assertEquals(0, m.size());
        assertTrue(m.isEmpty());
    }

    @Test
    void plusAddsEntryWithoutMutatingOriginal() {
        HashTrieMap<String, Integer> m0 = HashTrieMap.empty();
        HashTrieMap<String, Integer> m1 = m0.plus("a", 1);

        assertEquals(0, m0.size());
        assertFalse(m0.containsKey("a"));
        assertEquals(1, m1.size());
        assertEquals(1, m1.get("a"));
    }

    @Test
    void plusWithSameKeyValueReturnsSameInstance() {
        HashTrieMap<String, Integer> m1 = HashTrieMap.<String, Integer>empty().plus("a", 1);
        HashTrieMap<String, Integer> m2 = m1.plus("a", 1);

        assertSame(m1, m2);
    }

    @Test
    void plusReplacesExistingKey() {
        HashTrieMap<String, Integer> m1 = HashTrieMap.<String, Integer>empty().plus("a", 1);
        HashTrieMap<String, Integer> m2 = m1.plus("a", 2);

        assertEquals(1, m1.get("a"));
        assertEquals(2, m2.get("a"));
        assertEquals(1, m2.size());
    }

    @Test
    void minusRemovesKeyWithoutMutatingOriginal() {
        HashTrieMap<String, Integer> m1 = HashTrieMap.<String, Integer>empty().plus("a", 1).plus("b", 2);
        HashTrieMap<String, Integer> m2 = m1.minus("a");

        assertEquals(2, m1.size());
        assertTrue(m1.containsKey("a"));
        assertEquals(1, m2.size());
        assertFalse(m2.containsKey("a"));
        assertNull(m2.get("a"));
    }

    @Test
    void minusOnAbsentKeyReturnsSameInstance() {
        HashTrieMap<String, Integer> m = HashTrieMap.<String, Integer>empty().plus("a", 1);
        assertSame(m, m.minus("missing"));
    }

    @Test
    void toleratesManyEntriesAcrossMultipleTrieLevels() {
        HashTrieMap<Integer, Integer> m = HashTrieMap.empty();
        for (int i = 0; i < 5_000; i++) {
            m = m.plus(i, i * 2);
        }
        assertEquals(5_000, m.size());
        for (int i = 0; i < 5_000; i++) {
            assertEquals(i * 2, m.get(i));
        }
    }

    // Two keys engineered to share the same mixed hash, forcing the HAMT collision path.
    private record CollidingKey(String label) {
        @Override
        public int hashCode() {
            return 42;
        }
    }

    @Test
    void handlesFullHashCollisionsCorrectly() {
        CollidingKey k1 = new CollidingKey("first");
        CollidingKey k2 = new CollidingKey("second");
        assertEquals(k1.hashCode(), k2.hashCode());

        HashTrieMap<CollidingKey, String> m = HashTrieMap.<CollidingKey, String>empty()
                .plus(k1, "value-1")
                .plus(k2, "value-2");

        assertEquals(2, m.size());
        assertEquals("value-1", m.get(k1));
        assertEquals("value-2", m.get(k2));

        HashTrieMap<CollidingKey, String> afterRemove = m.minus(k1);
        assertEquals(1, afterRemove.size());
        assertNull(afterRemove.get(k1));
        assertEquals("value-2", afterRemove.get(k2));
        // original untouched
        assertEquals(2, m.size());
        assertEquals("value-1", m.get(k1));
    }

    @Test
    void structuralEqualityFollowsAbstractMapContract() {
        HashTrieMap<String, Integer> m1 = HashTrieMap.<String, Integer>empty().plus("a", 1).plus("b", 2);
        HashTrieMap<String, Integer> m2 = HashTrieMap.<String, Integer>empty().plus("b", 2).plus("a", 1);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
