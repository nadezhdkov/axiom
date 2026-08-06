package io.axiom.collections;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmortizedPQueueTest {

    @Test
    void emptyQueueHasNoElements() {
        PQueue<String> q = AmortizedPQueue.empty();
        assertEquals(0, q.size());
        assertTrue(q.isEmpty());
        assertNull(q.peek());
    }

    @Test
    void plusEnqueuesWithoutMutatingOriginal() {
        PQueue<Integer> q0 = AmortizedPQueue.empty();
        PQueue<Integer> q1 = q0.plus(1).plus(2).plus(3);

        assertEquals(0, q0.size());
        assertEquals(1, q1.peek());
    }

    @Test
    void minusDequeuesInFifoOrder() {
        PQueue<Integer> q = AmortizedPQueue.<Integer>empty().plus(1).plus(2).plus(3);

        assertEquals(1, q.peek());
        q = q.minus();
        assertEquals(2, q.peek());
        q = q.minus();
        assertEquals(3, q.peek());
        q = q.minus();
        assertTrue(q.isEmpty());
    }

    @Test
    void minusOnEmptyQueueReturnsThis() {
        PQueue<Integer> empty = AmortizedPQueue.empty();
        assertEquals(empty, empty.minus());
    }

    @Test
    void iteratorFollowsFifoOrderEvenBeforeNormalization() {
        // Enqueue-only sequence: elements sit in `back` in reverse, iterator must still yield
        // FIFO order without requiring an explicit dequeue first.
        PQueue<Integer> q = AmortizedPQueue.<Integer>empty().plus(1).plus(2).plus(3);
        assertEquals(java.util.List.of(1, 2, 3), java.util.List.copyOf(q));
    }

    @Test
    void interleavedEnqueueDequeuePreservesFifoOrder() {
        PQueue<Integer> q = AmortizedPQueue.empty();
        q = q.plus(1).plus(2);
        q = q.minus(); // removes 1
        q = q.plus(3);
        assertEquals(java.util.List.of(2, 3), java.util.List.copyOf(q));
    }

    @Test
    void minusByValueRemovesFirstMatch() {
        PQueue<Integer> q = AmortizedPQueue.<Integer>empty().plus(1).plus(2).plus(1);
        PQueue<Integer> updated = q.minus(Integer.valueOf(1));
        assertEquals(java.util.List.of(2, 1), java.util.List.copyOf(updated));
    }

    @Test
    void nullElementsAreRejected() {
        assertThrows(NullPointerException.class, () -> AmortizedPQueue.empty().plus(null));
    }

    @Test
    void elementThrowsOnEmptyQueueInsteadOfReturningNull() {
        // Regression guard for a bug in the reference this was ported from: `element()` there
        // silently returned null on an empty queue instead of following the Queue contract.
        assertThrows(NoSuchElementException.class, () -> AmortizedPQueue.empty().element());
    }

    @Test
    void mutatingQueueMethodsThrow() {
        PQueue<Integer> q = AmortizedPQueue.<Integer>empty().plus(1);
        assertThrows(UnsupportedOperationException.class, () -> q.offer(2));
        assertThrows(UnsupportedOperationException.class, q::poll);
        assertThrows(UnsupportedOperationException.class, q::remove);
    }
}
