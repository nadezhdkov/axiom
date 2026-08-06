package io.axiom.collections;

import io.axiom.collections.hamt.Hashing;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Persistent (immutable) hash map implemented as a HAMT (Hash Array Mapped Trie).
 *
 * <h2>Overview</h2>
 * {@code HashTrieMap} is an immutable map that provides efficient lookups and updates while
 * preserving previous versions through structural sharing: only the nodes along the modified
 * path are copied on {@link #plus(Object, Object)}/{@link #minus(Object)}; unchanged subtrees
 * are reused across versions.
 *
 * <h2>Complexity</h2>
 * {@link #get(Object)}, {@link #containsKey(Object)}, {@link #plus(Object, Object)} and
 * {@link #minus(Object)} are {@code O(1)} expected (bounded by {@code O(log32 n)} trie depth).
 * Keys that collide on their full mixed hash are stored linearly in a {@code CollisionNode}.
 *
 * @param <K> key type
 * @param <V> value type
 * @see PMap
 * @see Hashing
 */
public final class HashTrieMap<K, V> extends AbstractMap<K, V> implements PMap<K, V>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final HashTrieMap<?, ?> EMPTY = new HashTrieMap<>(Node.empty(), 0);

    private final Node<K, V> root;
    private final int size;

    private HashTrieMap(Node<K, V> root, int size) {
        this.root = root;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> HashTrieMap<K, V> empty() {
        return (HashTrieMap<K, V>) EMPTY;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean containsKey(Object key) {
        return root.get(Hashing.hash(key), key, 0) != Node.NOT_FOUND;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        Object v = root.get(Hashing.hash(key), key, 0);
        return v == Node.NOT_FOUND ? null : (V) v;
    }

    @Override
    public HashTrieMap<K, V> plus(K key, V value) {
        int h = Hashing.hash(key);
        Box added = new Box(false);
        Node<K, V> newRoot = root.put(h, key, value, 0, added);
        if (newRoot == root) return this;
        return new HashTrieMap<>(newRoot, added.value ? size + 1 : size);
    }

    @Override
    public HashTrieMap<K, V> plusAll(Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map");
        HashTrieMap<K, V> out = this;
        for (Entry<? extends K, ? extends V> e : map.entrySet()) {
            out = out.plus(e.getKey(), e.getValue());
        }
        return out;
    }

    @Override
    public HashTrieMap<K, V> minus(Object key) {
        int h = Hashing.hash(key);
        Box removed = new Box(false);
        Node<K, V> newRoot = root.remove(h, key, 0, removed);
        if (!removed.value) return this;
        return new HashTrieMap<>(newRoot, size - 1);
    }

    @Override
    public HashTrieMap<K, V> minusAll(Collection<?> keys) {
        Objects.requireNonNull(keys, "keys");
        HashTrieMap<K, V> out = this;
        for (Object k : keys) out = out.minus(k);
        return out;
    }

    private transient Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new AbstractSet<>() {
                @Override
                public int size() {
                    return HashTrieMap.this.size;
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    ArrayList<Entry<K, V>> out = new ArrayList<>(HashTrieMap.this.size);
                    root.forEach(out::add);
                    return Collections.unmodifiableList(out).iterator();
                }

                @Override
                public boolean contains(Object o) {
                    if (!(o instanceof Entry<?, ?> e)) return false;
                    Object k = e.getKey();
                    if (!HashTrieMap.this.containsKey(k)) return false;
                    return Objects.equals(HashTrieMap.this.get(k), e.getValue());
                }
            };
        }
        return entrySet;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        root.forEach(e -> action.accept(e.getKey(), e.getValue()));
    }

    @Deprecated
    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("immutable");
    }

    @Deprecated
    @Override
    public void clear() {
        throw new UnsupportedOperationException("immutable");
    }

    private static final class Box {
        boolean value;

        Box(boolean v) {
            this.value = v;
        }
    }

    /**
     * Internal HAMT node: {@code EmptyNode} (no mappings), {@code LeafNode} (single pair),
     * {@code CollisionNode} (multiple entries sharing a full hash), or {@code BitmapIndexedNode}
     * (main branching node, bitmap + dense children array).
     */
    private interface Node<K, V> {
        Object NOT_FOUND = new Object();

        Object get(int hash, Object key, int shift);

        Node<K, V> put(int hash, K key, V value, int shift, Box added);

        Node<K, V> remove(int hash, Object key, int shift, Box removed);

        void forEach(Consumer<Entry<K, V>> sink);

        static <K, V> Node<K, V> empty() {
            return EmptyNode.instance();
        }

        default Node<K, V> putAllCollision(CollisionNode<K, V> col, int shift) {
            Node<K, V> n = this;
            for (int i = 0; i < col.keys.length; i++) {
                @SuppressWarnings("unchecked") K k = (K) col.keys[i];
                @SuppressWarnings("unchecked") V v = (V) col.values[i];
                n = n.put(col.hash, k, v, shift, new Box(false));
            }
            return n;
        }
    }

    private static final class EmptyNode<K, V> implements Node<K, V>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private static final EmptyNode<?, ?> INSTANCE = new EmptyNode<>();

        @SuppressWarnings("unchecked")
        static <K, V> EmptyNode<K, V> instance() {
            return (EmptyNode<K, V>) INSTANCE;
        }

        @Override
        public Object get(int hash, Object key, int shift) {
            return NOT_FOUND;
        }

        @Override
        public Node<K, V> put(int hash, K key, V value, int shift, Box added) {
            added.value = true;
            return new LeafNode<>(hash, key, value);
        }

        @Override
        public Node<K, V> remove(int hash, Object key, int shift, Box removed) {
            return this;
        }

        @Override
        public void forEach(Consumer<Entry<K, V>> sink) {
            /* none */
        }
    }

    private record LeafNode<K, V>(int hash, K key, V value) implements Node<K, V>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(int hash, Object key, int shift) {
            if (this.hash == hash && Objects.equals(this.key, key)) return value;
            return NOT_FOUND;
        }

        @Override
        public Node<K, V> put(int hash, K key, V value, int shift, Box added) {
            if (this.hash == hash) {
                if (Objects.equals(this.key, key)) {
                    if (Objects.equals(this.value, value)) return this;
                    return new LeafNode<>(hash, key, value);
                }

                added.value = true;
                return new CollisionNode<>(hash, new Object[]{this.key, key}, new Object[]{this.value, value});
            }

            added.value = true;
            return BitmapIndexedNode.mergeLeaves(this, new LeafNode<>(hash, key, value), shift);
        }

        @Override
        public Node<K, V> remove(int hash, Object key, int shift, Box removed) {
            if (this.hash == hash && Objects.equals(this.key, key)) {
                removed.value = true;
                return EmptyNode.instance();
            }
            return this;
        }

        @Override
        public void forEach(Consumer<Entry<K, V>> sink) {
            sink.accept(new SimpleImmutableEntry<>(key, value));
        }
    }

    private record CollisionNode<K, V>(int hash, Object[] keys, Object[] values) implements Node<K, V>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(int hash, Object key, int shift) {
            if (this.hash != hash) return NOT_FOUND;
            for (int i = 0; i < keys.length; i++) {
                if (Objects.equals(keys[i], key)) return values[i];
            }
            return NOT_FOUND;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Node<K, V> put(int hash, K key, V value, int shift, Box added) {
            if (this.hash != hash) {
                added.value = true;
                return BitmapIndexedNode.mergeLeaves(
                                new LeafNode<>(this.hash, (K) keys[0], (V) values[0]),
                                new LeafNode<>(hash, key, value),
                                shift
                        ).put(this.hash, (K) keys[1], (V) values[1], shift, new Box(false))
                        .putAllCollision(this, shift);
            }

            for (int i = 0; i < keys.length; i++) {
                if (Objects.equals(keys[i], key)) {
                    if (Objects.equals(values[i], value)) return this;
                    Object[] nk = keys.clone();
                    Object[] nv = values.clone();
                    nk[i] = key;
                    nv[i] = value;
                    return new CollisionNode<>(hash, nk, nv);
                }
            }
            added.value = true;
            Object[] nk = Arrays.copyOf(keys, keys.length + 1);
            Object[] nv = Arrays.copyOf(values, values.length + 1);
            nk[keys.length] = key;
            nv[values.length] = value;
            return new CollisionNode<>(hash, nk, nv);
        }

        @Override
        public Node<K, V> remove(int hash, Object key, int shift, Box removed) {
            if (this.hash != hash) return this;

            int idx = -1;
            for (int i = 0; i < keys.length; i++) {
                if (Objects.equals(keys[i], key)) {
                    idx = i;
                    break;
                }
            }
            if (idx == -1) return this;

            removed.value = true;

            if (keys.length == 2) {
                int other = idx == 0 ? 1 : 0;
                @SuppressWarnings("unchecked") K ok = (K) keys[other];
                @SuppressWarnings("unchecked") V ov = (V) values[other];
                return new LeafNode<>(hash, ok, ov);
            }

            Object[] nk = new Object[keys.length - 1];
            Object[] nv = new Object[values.length - 1];
            int p = 0;
            for (int i = 0; i < keys.length; i++) {
                if (i == idx) continue;
                nk[p] = keys[i];
                nv[p] = values[i];
                p++;
            }
            return new CollisionNode<>(hash, nk, nv);
        }

        @Override
        public void forEach(Consumer<Entry<K, V>> sink) {
            for (int i = 0; i < keys.length; i++) {
                @SuppressWarnings("unchecked") K k = (K) keys[i];
                @SuppressWarnings("unchecked") V v = (V) values[i];
                sink.accept(new SimpleImmutableEntry<>(k, v));
            }
        }
    }

    /**
     * Bitmap-indexed branching node: {@code bitmap} encodes which of the 32 possible child
     * slots are populated at this level; children are stored densely, and
     * {@link Hashing#index(int, int)} translates a logical bit position into a physical index.
     */
    private record BitmapIndexedNode<K, V>(int bitmap, Node<K, V>[] children) implements Node<K, V>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Object get(int hash, Object key, int shift) {
            int m = Hashing.mask(hash, shift);
            int bit = Hashing.bitShift(m);
            if ((bitmap & bit) == 0) return NOT_FOUND;
            int idx = Hashing.index(bitmap, bit);
            return children[idx].get(hash, key, shift + 5);
        }

        @Override
        public Node<K, V> put(int hash, K key, V value, int shift, Box added) {
            int m = Hashing.mask(hash, shift);
            int bit = Hashing.bitShift(m);
            int idx = Hashing.index(bitmap, bit);

            if ((bitmap & bit) == 0) {
                @SuppressWarnings("unchecked")
                Node<K, V>[] newChildren = (Node<K, V>[]) new Node[children.length + 1];
                System.arraycopy(children, 0, newChildren, 0, idx);
                newChildren[idx] = new LeafNode<>(hash, key, value);
                System.arraycopy(children, idx, newChildren, idx + 1, children.length - idx);
                added.value = true;
                return new BitmapIndexedNode<>(bitmap | bit, newChildren);
            }

            Node<K, V> child = children[idx];
            Node<K, V> newChild = child.put(hash, key, value, shift + 5, added);
            if (newChild == child) return this;

            Node<K, V>[] newChildren = children.clone();
            newChildren[idx] = newChild;
            return new BitmapIndexedNode<>(bitmap, newChildren);
        }

        @Override
        public Node<K, V> remove(int hash, Object key, int shift, Box removed) {
            int m = Hashing.mask(hash, shift);
            int bit = Hashing.bitShift(m);
            if ((bitmap & bit) == 0) return this;

            int idx = Hashing.index(bitmap, bit);
            Node<K, V> child = children[idx];
            Node<K, V> newChild = child.remove(hash, key, shift + 5, removed);
            if (newChild == child) return this;

            if (newChild instanceof EmptyNode) {
                int newBitmap = bitmap & ~bit;
                if (newBitmap == 0) return EmptyNode.instance();

                @SuppressWarnings("unchecked")
                Node<K, V>[] newChildren = (Node<K, V>[]) new Node[children.length - 1];
                System.arraycopy(children, 0, newChildren, 0, idx);
                System.arraycopy(children, idx + 1, newChildren, idx, children.length - idx - 1);

                if (newChildren.length == 1 && newChildren[0] instanceof LeafNode<?, ?> leaf) {
                    @SuppressWarnings("unchecked") Node<K, V> one = (Node<K, V>) leaf;
                    return one;
                }
                return new BitmapIndexedNode<>(newBitmap, newChildren);
            } else {
                Node<K, V>[] newChildren = children.clone();
                newChildren[idx] = newChild;
                return new BitmapIndexedNode<>(bitmap, newChildren);
            }
        }

        @Override
        public void forEach(Consumer<Entry<K, V>> sink) {
            for (Node<K, V> c : children) c.forEach(sink);
        }

        static <K, V> Node<K, V> mergeLeaves(LeafNode<K, V> a, LeafNode<K, V> b, int shift) {
            int am = Hashing.mask(a.hash, shift);
            int bm = Hashing.mask(b.hash, shift);
            int abit = Hashing.bitShift(am);
            int bbit = Hashing.bitShift(bm);

            if (am != bm) {
                @SuppressWarnings("unchecked")
                Node<K, V>[] kids = (Node<K, V>[]) new Node[2];
                int bitmap = abit | bbit;

                if (am < bm) {
                    kids[0] = a;
                    kids[1] = b;
                } else {
                    kids[0] = b;
                    kids[1] = a;
                }
                return new BitmapIndexedNode<>(bitmap, kids);
            }

            Node<K, V> sub = mergeLeaves(a, b, shift + 5);
            @SuppressWarnings("unchecked")
            Node<K, V>[] kids = (Node<K, V>[]) new Node[]{sub};
            return new BitmapIndexedNode<>(abit, kids);
        }
    }
}
