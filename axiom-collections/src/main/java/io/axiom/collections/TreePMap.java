package io.axiom.collections;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

/**
 * Persistent (immutable) sorted map backed by an unmodifiable {@link NavigableMap} snapshot,
 * copy-on-write on every mutation.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class TreePMap<K, V> extends AbstractMap<K, V> implements PSortedMap<K, V> {

    private final NavigableMap<K, V> map;
    private final Comparator<? super K> comparator;

    private transient Set<Entry<K, V>> entrySetCache;

    private TreePMap(NavigableMap<K, V> map, Comparator<? super K> comparator) {
        this.map = requireNonNull(map, "map");
        this.comparator = requireNonNull(comparator, "comparator");
    }

    public static <K extends Comparable<? super K>, V> TreePMap<K, V> empty() {
        return empty(Comparator.naturalOrder());
    }

    public static <K, V> TreePMap<K, V> empty(Comparator<? super K> comparator) {
        requireNonNull(comparator, "comparator");
        return new TreePMap<>(Collections.unmodifiableNavigableMap(new TreeMap<>(comparator)), comparator);
    }

    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public TreePMap<K, V> plus(K key, V value) {
        requireNonNull(key, "key is null");
        if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
            return this;
        }

        TreeMap<K, V> tm = new TreeMap<>(comparator);
        tm.putAll(map);
        tm.put(key, value);
        return new TreePMap<>(Collections.unmodifiableNavigableMap(tm), comparator);
    }

    @Override
    public TreePMap<K, V> plusAll(Map<? extends K, ? extends V> m) {
        requireNonNull(m, "map is null");
        if (m.isEmpty()) {
            return this;
        }

        TreeMap<K, V> tm = new TreeMap<>(comparator);
        tm.putAll(map);
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            tm.put(requireNonNull(e.getKey(), "map contains null key"), e.getValue());
        }
        return new TreePMap<>(Collections.unmodifiableNavigableMap(tm), comparator);
    }

    @Override
    public TreePMap<K, V> minus(Object key) {
        requireNonNull(key, "key is null");
        if (!map.containsKey(key)) {
            return this;
        }

        TreeMap<K, V> tm = new TreeMap<>(comparator);
        tm.putAll(map);
        tm.remove(key);
        return new TreePMap<>(Collections.unmodifiableNavigableMap(tm), comparator);
    }

    @Override
    public TreePMap<K, V> minusAll(Collection<?> keys) {
        requireNonNull(keys, "keys is null");
        if (keys.isEmpty()) {
            return this;
        }

        TreeMap<K, V> tm = new TreeMap<>(comparator);
        tm.putAll(map);
        boolean changed = false;
        for (Object k : keys) {
            if (tm.containsKey(k)) {
                tm.remove(k);
                changed = true;
            }
        }
        return changed ? new TreePMap<>(Collections.unmodifiableNavigableMap(tm), comparator) : this;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySetCache == null) {
            entrySetCache = Collections.unmodifiableSet(map.entrySet());
        }
        return entrySetCache;
    }

    @Override
    public K firstKey() {
        return map.firstKey();
    }

    @Override
    public K lastKey() {
        return map.lastKey();
    }

    @Override
    public Entry<K, V> firstEntry() {
        return map.firstEntry();
    }

    @Override
    public Entry<K, V> lastEntry() {
        return map.lastEntry();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        return map.lowerEntry(key);
    }

    @Override
    public K lowerKey(K key) {
        return map.lowerKey(key);
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        return map.floorEntry(key);
    }

    @Override
    public K floorKey(K key) {
        return map.floorKey(key);
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        return map.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(K key) {
        return map.ceilingKey(key);
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        return map.higherEntry(key);
    }

    @Override
    public K higherKey(K key) {
        return map.higherKey(key);
    }

    @Override
    public TreePMap<K, V> descendingMap() {
        NavigableMap<K, V> dm = map.descendingMap();
        return new TreePMap<>(Collections.unmodifiableNavigableMap(new TreeMap<>(dm)), comparator.reversed());
    }

    @Override
    public TreePSet<K> navigableKeySet() {
        return keySetOf(map.navigableKeySet(), comparator);
    }

    @Override
    public TreePSet<K> descendingKeySet() {
        return keySetOf(map.descendingKeySet(), comparator.reversed());
    }

    private static <K> TreePSet<K> keySetOf(java.util.NavigableSet<K> keys, Comparator<? super K> cmp) {
        TreePSet<K> out = TreePSet.empty(cmp);
        return out.plusAll(keys);
    }

    @Override
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return map.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return map.headMap(toKey, inclusive);
    }

    @Override
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return map.tailMap(fromKey, inclusive);
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
        // Fixed vs. the reference this was ported from, which returned null here.
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException("immutable");
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
}
