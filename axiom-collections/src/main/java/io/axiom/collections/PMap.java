package io.axiom.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent (immutable) map abstraction.
 *
 * <h2>Overview</h2>
 * {@code PMap} represents an immutable key-value map. Modification operations return a new
 * {@code PMap} instance, preserving the original map unchanged and sharing unmodified
 * structure with it.
 *
 * <h2>Empty instance</h2>
 * {@link #empty()} returns the canonical empty persistent map, backed by {@link HashTrieMap}.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see HashTrieMap
 */
public interface PMap<K, V> extends Map<K, V> {

    PMap<K, V> plus(K key, V value);

    PMap<K, V> plusAll(Map<? extends K, ? extends V> map);

    PMap<K, V> minus(Object key);

    PMap<K, V> minusAll(Collection<?> keys);

    default Optional<V> getOpt(K key) {
        return Optional.ofNullable(get(key));
    }

    /** @deprecated Persistent maps are immutable. Use {@link #plus(Object, Object)} instead. */
    @Deprecated
    @Override
    V put(K key, V value);

    /** @deprecated Persistent maps are immutable. Use {@link #minus(Object)} instead. */
    @Deprecated
    @Override
    V remove(Object key);

    /** @deprecated Persistent maps are immutable. Use {@link #plusAll(Map)} instead. */
    @Deprecated
    @Override
    void putAll(Map<? extends K, ? extends V> m);

    /** @deprecated Persistent maps are immutable; this operation is not supported. */
    @Deprecated
    @Override
    void clear();

    static <K, V> PMap<K, V> empty() {
        return HashTrieMap.empty();
    }
}
