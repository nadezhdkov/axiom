package io.axiom.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Persistent (immutable) cons-list stack: {@link #plus(Object)} (push) is {@code O(1)} with full
 * structural sharing of the tail; random access ({@link #get(int)}) and every other
 * {@link PSequence} operation is {@code O(n)}, since they all walk the linked structure.
 *
 * <p>Null elements are not supported.
 *
 * @param <E> the element type
 */
public final class ConsPStack<E> extends AbstractList<E> implements PStack<E> {

    private static final ConsPStack<?> EMPTY = new ConsPStack<>(null, null, 0);

    private final E head;
    private final ConsPStack<E> tail;
    private final int size;

    private ConsPStack(E head, ConsPStack<E> tail, int size) {
        this.head = head;
        this.tail = tail;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <E> ConsPStack<E> empty() {
        return (ConsPStack<E>) EMPTY;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(int index) {
        Objects.checkIndex(index, size);
        ConsPStack<E> cur = this;
        for (int i = 0; i < index; i++) {
            cur = cur.tail;
        }
        return cur.head;
    }

    @Override
    public PStack<E> plus(E e) {
        Objects.requireNonNull(e, "null elements are not supported");
        return new ConsPStack<>(e, this, size + 1);
    }

    @Override
    public PStack<E> plusAll(Collection<? extends E> list) {
        Objects.requireNonNull(list, "list");
        Object[] arr = list.toArray();
        PStack<E> s = this;
        for (int i = arr.length - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked") E e = (E) arr[i];
            s = s.plus(e);
        }
        return s;
    }

    @Override
    public PStack<E> with(int i, E e) {
        Objects.checkIndex(i, size);
        Objects.requireNonNull(e, "null elements are not supported");
        if (i == 0) {
            return new ConsPStack<>(e, tail, size);
        }
        return rebuildWith(i, e, this);
    }

    private static <E> PStack<E> rebuildWith(int i, E e, ConsPStack<E> from) {
        List<E> prefix = new ArrayList<>(i);
        ConsPStack<E> cur = from;
        for (int idx = 0; idx < i; idx++) {
            prefix.add(cur.head);
            cur = cur.tail;
        }

        PStack<E> out = cur.tail;
        out = out.plus(e);
        for (int idx = prefix.size() - 1; idx >= 0; idx--) {
            out = out.plus(prefix.get(idx));
        }
        return out;
    }

    @Override
    public PStack<E> plus(int i, E e) {
        Objects.requireNonNull(e, "null elements are not supported");
        if (i == 0) {
            return plus(e);
        }
        if (i == size) {
            return plusAllAtEnd(List.of(e));
        }
        Objects.checkIndex(i, size);
        return insertAt(i, e);
    }

    private PStack<E> insertAt(int i, E e) {
        List<E> prefix = new ArrayList<>(i);
        ConsPStack<E> cur = this;
        for (int idx = 0; idx < i; idx++) {
            prefix.add(cur.head);
            cur = cur.tail;
        }
        PStack<E> out = cur;
        out = out.plus(e);
        for (int idx = prefix.size() - 1; idx >= 0; idx--) {
            out = out.plus(prefix.get(idx));
        }
        return out;
    }

    private PStack<E> plusAllAtEnd(Collection<? extends E> tailItems) {
        ArrayList<E> all = new ArrayList<>(size + tailItems.size());
        all.addAll(this);
        all.addAll(tailItems);

        PStack<E> s = empty();
        for (int idx = all.size() - 1; idx >= 0; idx--) {
            s = s.plus(all.get(idx));
        }
        return s;
    }

    @Override
    public PStack<E> plusAll(int i, Collection<? extends E> list) {
        Objects.requireNonNull(list, "list");
        PStack<E> out = this;
        int idx = i;
        for (E e : list) {
            out = out.plus(idx, e);
            idx++;
        }
        return out;
    }

    @Override
    public PStack<E> minus(Object e) {
        if (size == 0) {
            return this;
        }
        ArrayList<E> all = new ArrayList<>(size);
        boolean removed = false;
        for (E x : this) {
            if (!removed && Objects.equals(x, e)) {
                removed = true;
                continue;
            }
            all.add(x);
        }
        if (!removed) {
            return this;
        }
        PStack<E> s = empty();
        for (int idx = all.size() - 1; idx >= 0; idx--) {
            s = s.plus(all.get(idx));
        }
        return s;
    }

    @Override
    public PStack<E> minusAll(Collection<?> list) {
        Objects.requireNonNull(list, "list");
        if (list.isEmpty() || size == 0) {
            return this;
        }
        ArrayList<E> all = new ArrayList<>(size);
        for (E x : this) {
            if (!list.contains(x)) {
                all.add(x);
            }
        }
        PStack<E> s = empty();
        for (int idx = all.size() - 1; idx >= 0; idx--) {
            s = s.plus(all.get(idx));
        }
        return s;
    }

    @Override
    public PStack<E> minus(int i) {
        Objects.checkIndex(i, size);
        ArrayList<E> all = new ArrayList<>(size - 1);
        for (int idx = 0; idx < size; idx++) {
            if (idx != i) {
                all.add(get(idx));
            }
        }
        PStack<E> s = empty();
        for (int idx = all.size() - 1; idx >= 0; idx--) {
            s = s.plus(all.get(idx));
        }
        return s;
    }

    @Override
    public PStack<E> subList(int start, int end) {
        Objects.checkFromToIndex(start, end, size);
        ArrayList<E> all = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            all.add(get(i));
        }
        PStack<E> s = empty();
        for (int idx = all.size() - 1; idx >= 0; idx--) {
            s = s.plus(all.get(idx));
        }
        return s;
    }

    @Override
    public PStack<E> subList(int start) {
        return subList(start, size);
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

    @Deprecated
    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException("immutable");
    }
}
