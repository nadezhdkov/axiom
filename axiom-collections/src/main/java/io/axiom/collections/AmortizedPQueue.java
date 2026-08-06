package io.axiom.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Persistent (immutable) FIFO queue backed by two {@link ConsPStack}s ({@code front}/{@code back}
 * — the classic Okasaki two-stack functional queue).
 *
 * <h2>Complexity</h2>
 * {@link #plus(Object)}, {@link #minus()}, and {@link #peek()} are all amortized {@code O(1)}:
 * the only {@code O(n)} step is reversing {@code back} into {@code front}, which happens only
 * when {@code front} runs out, so its cost is spread across the sequence of operations that
 * emptied it.
 *
 * <p>Null elements are not supported.
 *
 * @param <E> the element type
 */
public final class AmortizedPQueue<E> extends AbstractCollection<E> implements PQueue<E> {

    private static final AmortizedPQueue<?> EMPTY =
            new AmortizedPQueue<>(ConsPStack.empty(), ConsPStack.empty(), 0);

    private final PStack<E> front;
    private final PStack<E> back;
    private final int size;

    private AmortizedPQueue(PStack<E> front, PStack<E> back, int size) {
        this.front = front;
        this.back = back;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <E> AmortizedPQueue<E> empty() {
        return (AmortizedPQueue<E>) EMPTY;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Iterator<E> iterator() {
        List<E> out = new ArrayList<>(size);
        out.addAll(front);
        ArrayList<E> tmp = new ArrayList<>(back);
        Collections.reverse(tmp);
        out.addAll(tmp);
        return Collections.unmodifiableList(out).iterator();
    }

    @Override
    public E peek() {
        if (size == 0) {
            return null;
        }
        return normalized().front.get(0);
    }

    @Override
    public PQueue<E> plus(E e) {
        Objects.requireNonNull(e, "null elements are not supported");
        return new AmortizedPQueue<>(front, back.plus(e), size + 1).normalized();
    }

    @Override
    public PQueue<E> plusAll(Collection<? extends E> list) {
        Objects.requireNonNull(list, "list");
        PQueue<E> q = this;
        for (E e : list) {
            q = q.plus(e);
        }
        return q;
    }

    @Override
    public PQueue<E> minus() {
        if (size == 0) {
            return this;
        }
        AmortizedPQueue<E> q = normalized();
        PStack<E> newFront = q.front.minus(0);
        return new AmortizedPQueue<>(newFront, q.back, size - 1).normalized();
    }

    @Override
    public PQueue<E> minus(Object e) {
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

        PQueue<E> q = empty();
        for (E x : all) {
            q = q.plus(x);
        }
        return q;
    }

    @Override
    public PQueue<E> minusAll(Collection<?> list) {
        Objects.requireNonNull(list, "list");
        if (list.isEmpty() || size == 0) {
            return this;
        }

        PQueue<E> q = empty();
        for (E x : this) {
            if (!list.contains(x)) {
                q = q.plus(x);
            }
        }
        return q;
    }

    private AmortizedPQueue<E> normalized() {
        if (size == 0) {
            return empty();
        }
        if (!front.isEmpty()) {
            return this;
        }

        ArrayList<E> tmp = new ArrayList<>(back);
        PStack<E> nf = ConsPStack.empty();
        for (E e : tmp) {
            nf = nf.plus(e);
        }
        return new AmortizedPQueue<>(nf, ConsPStack.empty(), size);
    }

    @Deprecated
    @Override
    public boolean offer(E e) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public E poll() {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public E element() {
        if (size == 0) {
            throw new java.util.NoSuchElementException();
        }
        return peek();
    }

    @Deprecated
    @Override
    public E remove() {
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
