package io.axiom.collections;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkedPVectorTest {

    @Test
    void emptyVectorHasNoElements() {
        PVector<String> v = ChunkedPVector.empty();
        assertEquals(0, v.size());
        assertTrue(v.isEmpty());
    }

    @Test
    void plusAppendsWithoutMutatingOriginal() {
        PVector<Integer> v0 = ChunkedPVector.empty();
        PVector<Integer> v1 = v0.plus(1).plus(2).plus(3);

        assertEquals(0, v0.size());
        assertEquals(List.of(1, 2, 3), v1);
    }

    @Test
    void getIsCorrectAcrossAChunkBoundary() {
        PVector<Integer> v = ChunkedPVector.empty();
        for (int i = 0; i < 100; i++) {
            v = v.plus(i);
        }
        for (int i = 0; i < 100; i++) {
            assertEquals(i, v.get(i));
        }
    }

    @Test
    void withReplacesElementWithoutAffectingOriginal() {
        PVector<String> v = ChunkedPVector.<String>empty().plus("a").plus("b").plus("c");
        PVector<String> updated = v.with(1, "X");

        assertEquals(List.of("a", "X", "c"), updated);
        assertEquals(List.of("a", "b", "c"), v);
    }

    @Test
    void plusAtIndexInsertsInTheMiddle() {
        PVector<Integer> v = ChunkedPVector.<Integer>empty().plus(1).plus(2).plus(4);
        assertEquals(List.of(1, 2, 3, 4), v.plus(2, 3));
    }

    @Test
    void plusAtEndBehavesLikeAppend() {
        PVector<Integer> v = ChunkedPVector.<Integer>empty().plus(1).plus(2);
        assertEquals(List.of(1, 2, 3), v.plus(2, 3));
    }

    @Test
    void minusByIndexRemovesElement() {
        PVector<Integer> v = ChunkedPVector.<Integer>empty().plus(1).plus(2).plus(3);
        assertEquals(List.of(1, 3), v.minus(1));
    }

    @Test
    void minusByValueRemovesFirstOccurrence() {
        PVector<Integer> v = ChunkedPVector.<Integer>empty().plus(1).plus(2).plus(1);
        assertEquals(List.of(2, 1), v.minus(Integer.valueOf(1)));
    }

    @Test
    void subListMaterializesAPersistentSlice() {
        PVector<Integer> v = ChunkedPVector.<Integer>empty().plus(1).plus(2).plus(3).plus(4);
        assertEquals(List.of(2, 3), v.subList(1, 3));
    }

    @Test
    void fromListRoundTripsAcrossMultipleChunks() {
        List<Integer> source = new ArrayList<>();
        for (int i = 0; i < 70; i++) {
            source.add(i);
        }
        PVector<Integer> v = ChunkedPVector.fromList(source);
        assertEquals(source, v);
    }

    @Test
    void nullElementsAreRejected() {
        assertThrows(NullPointerException.class, () -> ChunkedPVector.empty().plus(null));
    }

    @Test
    void mutatingListMethodsThrow() {
        PVector<Integer> v = ChunkedPVector.<Integer>empty().plus(1);
        assertThrows(UnsupportedOperationException.class, () -> v.add(2));
        assertThrows(UnsupportedOperationException.class, () -> v.set(0, 5));
        assertThrows(UnsupportedOperationException.class, v::clear);
    }
}
