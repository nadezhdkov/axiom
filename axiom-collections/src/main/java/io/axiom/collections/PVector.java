package io.axiom.collections;

import java.util.Collection;

/**
 * Persistent (immutable) random-access vector abstraction.
 *
 * <h2>Overview</h2>
 * {@code PVector} is an immutable, {@code List}-like sequence optimized for random access
 * ({@link #get(int)}) and append ({@link #plus(Object)}).
 *
 * <h2>Empty instance</h2>
 * {@link #empty()} returns the canonical empty persistent vector, backed by
 * {@link ChunkedPVector}.
 *
 * @param <E> the element type
 * @see ChunkedPVector
 */
public interface PVector<E> extends PSequence<E> {

    @Override
    PVector<E> plus(E e);

    @Override
    PVector<E> plusAll(Collection<? extends E> list);

    @Override
    PVector<E> with(int index, E value);

    @Override
    PVector<E> plus(int index, E value);

    @Override
    PVector<E> plusAll(int index, Collection<? extends E> list);

    @Override
    PVector<E> minus(Object e);

    @Override
    PVector<E> minusAll(Collection<?> list);

    @Override
    PVector<E> minus(int index);

    @Override
    PVector<E> subList(int fromIndex, int toIndex);

    static <E> PVector<E> empty() {
        return ChunkedPVector.empty();
    }
}
