package io.axiom.collections;

import java.util.Collection;

/**
 * Persistent (immutable) collection abstraction.
 *
 * <h2>Overview</h2>
 * {@code PCollection} defines the base contract for all immutable / persistent collection
 * types in this module. Unlike standard {@link Collection} implementations, "modifying"
 * operations return a <em>new</em> collection instance instead of mutating the receiver.
 *
 * <h2>Immutability guarantees</h2>
 * Mutating methods inherited from {@link Collection} ({@link #add(Object)},
 * {@link #clear()}, ...) are deprecated; implementations throw
 * {@link UnsupportedOperationException} if they are called.
 *
 * @param <E> the element type
 */
public interface PCollection<E> extends Collection<E> {

    PCollection<E> plus(E e);

    PCollection<E> plusAll(Collection<? extends E> list);

    PCollection<E> minus(Object e);

    PCollection<E> minusAll(Collection<?> list);

    /** @deprecated Persistent collections are immutable. Use {@link #plus(Object)} instead. */
    @Deprecated
    @Override
    boolean add(E e);

    /** @deprecated Persistent collections are immutable. Use {@link #minus(Object)} instead. */
    @Deprecated
    @Override
    boolean remove(Object o);

    /** @deprecated Persistent collections are immutable. Use {@link #plusAll(Collection)} instead. */
    @Deprecated
    @Override
    boolean addAll(Collection<? extends E> c);

    /** @deprecated Persistent collections are immutable. Use {@link #minusAll(Collection)} instead. */
    @Deprecated
    @Override
    boolean removeAll(Collection<?> c);

    /** @deprecated Persistent collections are immutable; this operation is not supported. */
    @Deprecated
    @Override
    boolean retainAll(Collection<?> c);

    /** @deprecated Persistent collections are immutable; this operation is not supported. */
    @Deprecated
    @Override
    void clear();
}
