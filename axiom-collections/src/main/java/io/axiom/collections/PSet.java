package io.axiom.collections;

import java.util.Collection;
import java.util.Set;

/**
 * Persistent (immutable) set abstraction: a collection of unique elements, combining
 * {@link Set} semantics with {@link PCollection} immutability.
 *
 * @param <E> the element type
 * @see HashTrieSet
 */
public interface PSet<E> extends PCollection<E>, Set<E> {

    @Override
    PSet<E> plus(E e);

    @Override
    PSet<E> plusAll(Collection<? extends E> list);

    @Override
    PSet<E> minus(Object e);

    @Override
    PSet<E> minusAll(Collection<?> list);

    static <E> PSet<E> empty() {
        return HashTrieSet.empty();
    }
}
