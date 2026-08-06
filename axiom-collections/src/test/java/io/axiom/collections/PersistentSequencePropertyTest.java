package io.axiom.collections;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based invariants shared by the sequence-family persistent structures
 * ({@link ChunkedPVector}, {@link ConsPStack}) — axiom.md §11 treats "a prior version is never
 * affected by an operation derived from it" as non-negotiable for every persistent structure,
 * not just the HAMT map/set covered by {@link HashTrieMapPropertyTest}.
 */
class PersistentSequencePropertyTest {

    @Property
    void vectorAppendNeverAffectsThePriorVersion(@ForAll List<Integer> values) {
        PVector<Integer> before = ChunkedPVector.empty();
        for (int v : values) {
            before = before.plus(v);
        }

        PVector<Integer> after = before;
        for (int v : values) {
            after = after.plus(v);
        }

        assertEquals(values, before);
        assertEquals(values.size(), before.size());
        assertEquals(values.size() * 2, after.size());
    }

    @Property
    void vectorWithNeverAffectsThePriorVersion(@ForAll List<Integer> values) {
        if (values.isEmpty()) {
            return;
        }
        PVector<Integer> before = ChunkedPVector.fromList(values);
        PVector<Integer> after = before.with(0, -1);

        assertEquals(values.get(0), before.get(0));
        assertEquals(-1, after.get(0));
    }

    @Property
    void stackPushNeverAffectsThePriorVersion(@ForAll List<Integer> values) {
        PStack<Integer> before = ConsPStack.empty();
        for (int v : values) {
            before = before.plus(v);
        }

        PStack<Integer> after = before;
        for (int v : values) {
            after = after.plus(v);
        }

        assertEquals(before.size(), values.size());
        assertEquals(after.size(), values.size() * 2);
        // "before" must still report exactly its own elements, unaffected by building "after" from it
        for (int i = 0; i < before.size(); i++) {
            assertEquals(before.get(i), after.get(i));
        }
    }

    @Property
    void vectorFromListPreservesEncounterOrder(@ForAll List<Integer> values) {
        PVector<Integer> vector = ChunkedPVector.fromList(values);
        assertEquals(values, vector);
    }
}
