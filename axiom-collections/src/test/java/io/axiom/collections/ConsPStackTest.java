package io.axiom.collections;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsPStackTest {

    @Test
    void emptyStackHasNoElements() {
        PStack<String> s = ConsPStack.empty();
        assertEquals(0, s.size());
        assertTrue(s.isEmpty());
    }

    @Test
    void plusPushesAtIndexZeroWithoutMutatingOriginal() {
        PStack<Integer> s0 = ConsPStack.empty();
        PStack<Integer> s1 = s0.plus(1).plus(2).plus(3);

        assertEquals(0, s0.size());
        assertEquals(List.of(3, 2, 1), s1);
    }

    @Test
    void withReplacesElementAtIndex() {
        PStack<String> s = ConsPStack.<String>empty().plus("a").plus("b").plus("c");
        PStack<String> updated = s.with(1, "X");

        assertEquals(List.of("c", "X", "a"), updated);
        assertEquals(List.of("c", "b", "a"), s);
    }

    @Test
    void plusAtIndexInsertsWithoutShiftingWrongly() {
        PStack<Integer> s = ConsPStack.<Integer>empty().plus(1).plus(2).plus(3); // [3,2,1]
        PStack<Integer> updated = s.plus(1, 99);

        assertEquals(List.of(3, 99, 2, 1), updated);
    }

    @Test
    void minusByIndexRemovesElement() {
        PStack<Integer> s = ConsPStack.<Integer>empty().plus(1).plus(2).plus(3); // [3,2,1]
        assertEquals(List.of(3, 1), s.minus(1));
    }

    @Test
    void minusByValueRemovesFirstOccurrence() {
        PStack<Integer> s = ConsPStack.<Integer>empty().plus(1).plus(1).plus(2); // [2,1,1]
        assertEquals(List.of(2, 1), s.minus(Integer.valueOf(1)));
    }

    @Test
    void subListReturnsPersistentSlice() {
        PStack<Integer> s = ConsPStack.<Integer>empty().plus(1).plus(2).plus(3).plus(4); // [4,3,2,1]
        assertEquals(List.of(3, 2), s.subList(1, 3));
        assertEquals(List.of(3, 2, 1), s.subList(1));
    }

    @Test
    void nullElementsAreRejected() {
        assertThrows(NullPointerException.class, () -> ConsPStack.empty().plus(null));
    }

    @Test
    void mutatingListMethodsThrow() {
        PStack<Integer> s = ConsPStack.<Integer>empty().plus(1);
        assertThrows(UnsupportedOperationException.class, () -> s.add(2));
        assertThrows(UnsupportedOperationException.class, () -> s.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> s.set(0, 5));
        assertThrows(UnsupportedOperationException.class, s::clear);
    }
}
