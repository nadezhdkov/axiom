package io.axiom.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Persistent (immutable) {@link NavigableMap} with sorted keys.
 *
 * <h2>Empty instance</h2>
 * {@link #empty()} returns the canonical empty persistent sorted map (natural key ordering),
 * backed by {@link TreePMap}.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see TreePMap
 */
public interface PSortedMap<K, V> extends PMap<K, V>, NavigableMap<K, V> {

    @Override
    PSortedMap<K, V> plus(K key, V value);

    @Override
    PSortedMap<K, V> plusAll(Map<? extends K, ? extends V> map);

    @Override
    PSortedMap<K, V> minus(Object key);

    @Override
    PSortedMap<K, V> minusAll(Collection<?> keys);

    @Override
    PSortedMap<K, V> descendingMap();

    @Override
    PSortedSet<K> navigableKeySet();

    @Override
    PSortedSet<K> descendingKeySet();

    @Override
    Comparator<? super K> comparator();

    static <K extends Comparable<? super K>, V> PSortedMap<K, V> empty() {
        return TreePMap.empty();
    }

    static <K, V> PSortedMap<K, V> empty(Comparator<? super K> comparator) {
        return TreePMap.empty(comparator);
    }
}
