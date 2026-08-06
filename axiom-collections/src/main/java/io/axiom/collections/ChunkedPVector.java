package io.axiom.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Persistent (immutable) vector backed by fixed-size 32-element chunks.
 *
 * <h2>Design note — not an RRB-tree</h2>
 * This is a deliberate, documented simplification, not an oversight: {@code axiom.md} explicitly
 * gates this type between "implement a real RRB-tree" and "port the simpler 2-level chunked
 * design as-is, honestly documented" — this module takes the second option. Storage is a flat
 * {@code Object[][]} of 32-element chunks (2 levels only, not a real tree), so:
 * <ul>
 *   <li>{@link #get(int)} is {@code O(1)} — a chunk lookup via bit shifting.</li>
 *   <li>{@link #plus(Object)} (append) is amortized {@code O(1)}.</li>
 *   <li>{@link #with(int, Object)} is {@code O(1)} — copies only the outer array and one chunk.</li>
 *   <li>{@link #plus(int, Object)} / {@link #minus(int)} / {@link #subList(int, int)} are
 *       {@code O(n)} — they materialize a new vector via an intermediate list. A real RRB-tree
 *       would make middle insertion/removal {@code O(log n)}; this structure does not.</li>
 * </ul>
 *
 * <p>Null elements are not supported.
 *
 * @param <E> the element type
 */
public final class ChunkedPVector<E> extends AbstractList<E> implements PVector<E> {

    private static final int CHUNK_SHIFT = 5;
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    private static final ChunkedPVector<?> EMPTY = new ChunkedPVector<>(new Object[0][], 0);

    private final Object[][] chunks;
    private final int size;

    private ChunkedPVector(Object[][] chunks, int size) {
        this.chunks = chunks;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <E> ChunkedPVector<E> empty() {
        return (ChunkedPVector<E>) EMPTY;
    }

    @Override
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) {
        Objects.checkIndex(index, size);
        int ci = index >> CHUNK_SHIFT;
        int off = index & CHUNK_MASK;
        return (E) chunks[ci][off];
    }

    @Override
    public PVector<E> plus(E e) {
        Objects.requireNonNull(e, "null elements are not supported");

        int newSize = size + 1;
        int neededChunks = chunkCountForSize(newSize);

        Object[][] newChunks = chunks;
        if (neededChunks != chunks.length) {
            newChunks = Arrays.copyOf(chunks, neededChunks);
        }

        int lastIndex = newSize - 1;
        int ci = lastIndex >> CHUNK_SHIFT;
        int off = lastIndex & CHUNK_MASK;

        Object[] chunk = (ci < chunks.length) ? chunks[ci] : null;

        if (chunk == null || off == 0) {
            chunk = new Object[CHUNK_SIZE];
        } else {
            chunk = Arrays.copyOf(chunk, CHUNK_SIZE);
        }

        chunk[off] = e;
        newChunks[ci] = chunk;

        return new ChunkedPVector<>(newChunks, newSize);
    }

    @Override
    public PVector<E> plusAll(Collection<? extends E> list) {
        Objects.requireNonNull(list, "list");
        PVector<E> out = this;
        for (E e : list) {
            out = out.plus(e);
        }
        return out;
    }

    @Override
    public PVector<E> with(int index, E value) {
        Objects.checkIndex(index, size);
        Objects.requireNonNull(value, "null elements are not supported");

        int ci = index >> CHUNK_SHIFT;
        int off = index & CHUNK_MASK;

        Object[][] newChunks = Arrays.copyOf(chunks, chunks.length);
        Object[] newChunk = Arrays.copyOf(chunks[ci], CHUNK_SIZE);
        newChunk[off] = value;
        newChunks[ci] = newChunk;

        return new ChunkedPVector<>(newChunks, size);
    }

    @Override
    public PVector<E> plus(int index, E value) {
        Objects.requireNonNull(value, "null elements are not supported");
        Objects.checkIndex(index, size + 1);

        if (index == size) {
            return plus(value);
        }

        ArrayList<E> tmp = new ArrayList<>(size + 1);
        for (int i = 0; i < index; i++) {
            tmp.add(get(i));
        }
        tmp.add(value);
        for (int i = index; i < size; i++) {
            tmp.add(get(i));
        }

        return fromList(tmp);
    }

    @Override
    public PVector<E> plusAll(int index, Collection<? extends E> list) {
        Objects.requireNonNull(list, "list");
        Objects.checkIndex(index, size + 1);

        PVector<E> out = this;
        int i = index;
        for (E e : list) {
            out = out.plus(i, e);
            i++;
        }
        return out;
    }

    @Override
    public PVector<E> minus(Object e) {
        int idx = indexOf(e);
        if (idx < 0) {
            return this;
        }
        return minus(idx);
    }

    @Override
    public PVector<E> minus(int index) {
        Objects.checkIndex(index, size);
        ArrayList<E> tmp = new ArrayList<>(size - 1);
        for (int i = 0; i < size; i++) {
            if (i != index) {
                tmp.add(get(i));
            }
        }
        return fromList(tmp);
    }

    @Override
    public PVector<E> minusAll(Collection<?> list) {
        Objects.requireNonNull(list, "list");
        if (list.isEmpty() || size == 0) {
            return this;
        }

        ArrayList<E> tmp = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            E v = get(i);
            if (!list.contains(v)) {
                tmp.add(v);
            }
        }
        return fromList(tmp);
    }

    @Override
    public PVector<E> subList(int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, size);
        ArrayList<E> tmp = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            tmp.add(get(i));
        }
        return fromList(tmp);
    }

    private static int chunkCountForSize(int size) {
        if (size == 0) {
            return 0;
        }
        return ((size - 1) >> CHUNK_SHIFT) + 1;
    }

    /** Builds a {@code ChunkedPVector} from an existing list, read in encounter order. */
    public static <E> ChunkedPVector<E> fromList(List<? extends E> list) {
        Objects.requireNonNull(list, "list");
        if (list.isEmpty()) {
            return empty();
        }

        int size = list.size();
        int chunkCount = chunkCountForSize(size);
        Object[][] arr = new Object[chunkCount][];

        for (int ci = 0; ci < chunkCount; ci++) {
            Object[] chunk = new Object[CHUNK_SIZE];
            int base = ci * CHUNK_SIZE;
            int limit = Math.min(base + CHUNK_SIZE, size);
            for (int i = base; i < limit; i++) {
                Object v = list.get(i);
                if (v == null) {
                    throw new NullPointerException("null elements are not supported");
                }
                chunk[i - base] = v;
            }
            arr[ci] = chunk;
        }

        return new ChunkedPVector<>(arr, size);
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
    public boolean addAll(int index, Collection<? extends E> c) {
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

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException("immutable");
    }
}
