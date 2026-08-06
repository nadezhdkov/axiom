package io.axiom.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;

/**
 * Persistent (immutable) {@link NavigableSet} with sorted elements.
 *
 * <h2>Empty instance</h2>
 * {@link #empty()} returns the canonical empty persistent sorted set (natural ordering),
 * backed by {@link TreePSet}.
 *
 * @param <E> the element type
 * @see TreePSet
 */
public interface PSortedSet<E> extends PSet<E>, NavigableSet<E> {

    @Override
    PSortedSet<E> plus(E e);

    @Override
    PSortedSet<E> plusAll(Collection<? extends E> list);

    @Override
    PSortedSet<E> minus(Object e);

    @Override
    PSortedSet<E> minusAll(Collection<?> list);

    @Override
    Comparator<? super E> comparator();

    /** Returns a persistent sorted set with the reverse ordering of this set. */
    @Override
    PSortedSet<E> descendingSet();

    static <E extends Comparable<? super E>> PSortedSet<E> empty() {
        return TreePSet.empty();
    }

    static <E> PSortedSet<E> empty(Comparator<? super E> comparator) {
        return TreePSet.empty(comparator);
    }
}
