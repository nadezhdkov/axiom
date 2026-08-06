package io.axiom.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Objects.requireNonNull;

/**
 * Persistent (immutable) sorted set backed by an unmodifiable {@link NavigableSet} snapshot,
 * copy-on-write on every mutation.
 *
 * @param <E> the element type
 */
public final class TreePSet<E> extends AbstractSet<E> implements PSortedSet<E> {

    private final NavigableSet<E> set;
    private final Comparator<? super E> comparator;

    private TreePSet(NavigableSet<E> set, Comparator<? super E> comparator) {
        this.set = Collections.unmodifiableNavigableSet(requireNonNull(set, "set"));
        this.comparator = requireNonNull(comparator, "comparator");
    }

    public static <E extends Comparable<? super E>> TreePSet<E> empty() {
        return empty(Comparator.naturalOrder());
    }

    public static <E> TreePSet<E> empty(Comparator<? super E> comparator) {
        requireNonNull(comparator, "comparator");
        return new TreePSet<>(new TreeSet<>(comparator), comparator);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public TreePSet<E> plus(E e) {
        requireNonNull(e, "element is null");
        if (set.contains(e)) {
            return this;
        }

        TreeSet<E> ts = new TreeSet<>(comparator);
        ts.addAll(set);
        ts.add(e);
        return new TreePSet<>(ts, comparator);
    }

    @Override
    public TreePSet<E> plusAll(Collection<? extends E> list) {
        requireNonNull(list, "list");
        if (list.isEmpty()) {
            return this;
        }

        TreeSet<E> ts = new TreeSet<>(comparator);
        ts.addAll(set);
        boolean changed = false;
        for (E e : list) {
            requireNonNull(e, "list contains null");
            changed |= ts.add(e);
        }
        return changed ? new TreePSet<>(ts, comparator) : this;
    }

    @Override
    public TreePSet<E> minus(Object e) {
        requireNonNull(e, "element is null");
        if (!set.contains(e)) {
            return this;
        }

        TreeSet<E> ts = new TreeSet<>(comparator);
        ts.addAll(set);
        ts.remove(e);
        return new TreePSet<>(ts, comparator);
    }

    @Override
    public TreePSet<E> minusAll(Collection<?> list) {
        requireNonNull(list, "list");
        if (list.isEmpty()) {
            return this;
        }

        TreeSet<E> ts = new TreeSet<>(comparator);
        ts.addAll(set);
        boolean changed = ts.removeAll(list);
        return changed ? new TreePSet<>(ts, comparator) : this;
    }

    @Override
    public TreePSet<E> descendingSet() {
        return new TreePSet<>(set.descendingSet(), comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        // Fixed vs. the reference this was ported from, which returned null here (a landmine:
        // any caller following the standard NavigableSet contract would NPE on hasNext()).
        return set.descendingIterator();
    }

    @Override
    public E lower(E e) {
        return set.lower(e);
    }

    @Override
    public E floor(E e) {
        return set.floor(e);
    }

    @Override
    public E ceiling(E e) {
        return set.ceiling(e);
    }

    @Override
    public E higher(E e) {
        return set.higher(e);
    }

    @Override
    public E first() {
        return set.first();
    }

    @Override
    public E last() {
        return set.last();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return set.subSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return set.headSet(toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return set.tailSet(fromElement, inclusive);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        // Fixed vs. the reference this was ported from, which returned null here.
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("immutable");
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
