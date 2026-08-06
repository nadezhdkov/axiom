package io.axiom.collections;

import java.util.Collection;
import java.util.List;

/**
 * Persistent (immutable) indexed sequence abstraction: the shared contract behind
 * {@link PVector} and {@link PStack}.
 *
 * @param <E> the element type
 * @see PVector
 * @see PStack
 */
public interface PSequence<E> extends PCollection<E>, List<E> {

    @Override
    PSequence<E> plus(E e);

    @Override
    PSequence<E> plusAll(Collection<? extends E> list);

    /** Returns a new sequence where the element at {@code index} is replaced by {@code value}. */
    PSequence<E> with(int index, E value);

    /** Returns a new sequence with {@code value} inserted at {@code index}. */
    PSequence<E> plus(int index, E value);

    /** Returns a new sequence with all elements from {@code list} inserted starting at {@code index}. */
    PSequence<E> plusAll(int index, Collection<? extends E> list);

    @Override
    PSequence<E> minus(Object e);

    @Override
    PSequence<E> minusAll(Collection<?> list);

    /** Returns a new sequence with the element at {@code index} removed. */
    PSequence<E> minus(int index);

    @Override
    PSequence<E> subList(int fromIndex, int toIndex);

    /** @deprecated Persistent sequences are immutable. Use {@link #plusAll(int, Collection)} instead. */
    @Deprecated
    @Override
    boolean addAll(int index, Collection<? extends E> c);

    /** @deprecated Persistent sequences are immutable. Use {@link #with(int, Object)} instead. */
    @Deprecated
    @Override
    E set(int index, E element);

    /** @deprecated Persistent sequences are immutable. Use {@link #plus(int, Object)} instead. */
    @Deprecated
    @Override
    void add(int index, E element);

    /** @deprecated Persistent sequences are immutable. Use {@link #minus(int)} instead. */
    @Deprecated
    @Override
    E remove(int index);
}
