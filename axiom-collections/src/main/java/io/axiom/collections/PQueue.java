package io.axiom.collections;

import java.util.Collection;
import java.util.Queue;

/**
 * Persistent (immutable) FIFO queue abstraction.
 *
 * <h2>Removal semantics</h2>
 * {@link #minus()} removes the head (FIFO dequeue); {@link #minus(Object)} removes the first
 * occurrence of a matching element anywhere in the queue.
 *
 * <h2>Empty instance</h2>
 * {@link #empty()} returns the canonical empty persistent queue, backed by
 * {@link AmortizedPQueue}.
 *
 * @param <E> the element type
 * @see AmortizedPQueue
 */
public interface PQueue<E> extends PCollection<E>, Queue<E> {

    /** Returns a new queue with the head element removed, or {@code this} if empty. */
    PQueue<E> minus();

    @Override
    PQueue<E> plus(E e);

    @Override
    PQueue<E> plusAll(Collection<? extends E> list);

    @Override
    PQueue<E> minus(Object e);

    @Override
    PQueue<E> minusAll(Collection<?> list);

    /** @deprecated Persistent queues are immutable. Use {@link #plus(Object)} instead. */
    @Deprecated
    @Override
    boolean offer(E e);

    /** @deprecated Persistent queues are immutable. Use {@link #minus()} instead. */
    @Deprecated
    @Override
    E poll();

    /** @deprecated Persistent queues are immutable. Use {@link #minus()} instead. */
    @Deprecated
    @Override
    E remove();

    static <E> PQueue<E> empty() {
        return AmortizedPQueue.empty();
    }
}
