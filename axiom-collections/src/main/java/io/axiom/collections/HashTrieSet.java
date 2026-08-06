package io.axiom.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent (immutable) set backed by {@link HashTrieMap} (the standard "map with a sentinel
 * value" encoding of a HAMT set, mirroring {@code HashTrieMap}'s complexity characteristics).
 *
 * @param <E> the element type
 */
public final class HashTrieSet<E> extends AbstractSet<E> implements PSet<E>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Object PRESENT = new Object();
    private static final HashTrieSet<?> EMPTY = new HashTrieSet<>(HashTrieMap.empty());

    private final HashTrieMap<E, Object> map;

    private HashTrieSet(HashTrieMap<E, Object> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    public static <E> HashTrieSet<E> empty() {
        return (HashTrieSet<E>) EMPTY;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        Iterator<Map.Entry<E, Object>> it = map.entrySet().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return it.next().getKey();
            }
        };
    }

    @Override
    public HashTrieSet<E> plus(E e) {
        HashTrieMap<E, Object> m2 = map.plus(e, PRESENT);
        return m2 == map ? this : new HashTrieSet<>(m2);
    }

    @Override
    public HashTrieSet<E> plusAll(Collection<? extends E> list) {
        Objects.requireNonNull(list, "list");
        HashTrieSet<E> out = this;
        for (E e : list) out = out.plus(e);
        return out;
    }

    @Override
    public HashTrieSet<E> minus(Object e) {
        HashTrieMap<E, Object> m2 = map.minus(e);
        return m2 == map ? this : new HashTrieSet<>(m2);
    }

    @Override
    public HashTrieSet<E> minusAll(Collection<?> list) {
        Objects.requireNonNull(list, "list");
        HashTrieSet<E> out = this;
        for (Object e : list) out = out.minus(e);
        return out;
    }

    @Deprecated
    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException("immutable");
    }
}
