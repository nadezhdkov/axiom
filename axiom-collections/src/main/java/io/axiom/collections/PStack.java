package io.axiom.collections;

import java.util.Collection;

/**
 * Persistent (immutable) stack abstraction: a sequence optimized for LIFO push/pop at index 0,
 * while still supporting the full indexed {@link PSequence} contract.
 *
 * <h2>Empty instance</h2>
 * {@link #empty()} returns the canonical empty persistent stack, backed by {@link ConsPStack}.
 *
 * @param <E> the element type
 * @see ConsPStack
 */
public interface PStack<E> extends PSequence<E> {

    @Override
    PStack<E> plus(E e);

    @Override
    PStack<E> plusAll(Collection<? extends E> list);

    @Override
    PStack<E> with(int index, E value);

    @Override
    PStack<E> plus(int index, E value);

    @Override
    PStack<E> plusAll(int index, Collection<? extends E> list);

    @Override
    PStack<E> minus(Object e);

    @Override
    PStack<E> minusAll(Collection<?> list);

    @Override
    PStack<E> minus(int index);

    @Override
    PStack<E> subList(int fromIndex, int toIndex);

    /** Returns a new stack containing elements from {@code start} (inclusive) to the end. */
    PStack<E> subList(int start);

    static <E> PStack<E> empty() {
        return ConsPStack.empty();
    }
}
