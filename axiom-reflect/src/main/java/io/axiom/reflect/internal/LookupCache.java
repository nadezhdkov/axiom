package io.axiom.reflect.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Process-wide cache of {@link Field}/{@link Method} lookups keyed by owner class and name (and,
 * for methods, explicit parameter types when given). Never evicts — reflective members are
 * bounded by the set of loaded classes, not request volume.
 *
 * <p>This is new relative to the reference implementation this module was ported from, which
 * re-walked the class hierarchy with {@code getDeclaredField}/{@code getDeclaredMethods} on
 * every single lookup.
 */
public final class LookupCache {

    private static final ConcurrentHashMap<FieldKey, Field> FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MethodKey, Method> METHODS = new ConcurrentHashMap<>();

    private LookupCache() {
    }

    public static Field field(Class<?> owner, String name, Supplier<Field> loader) {
        return FIELDS.computeIfAbsent(new FieldKey(owner, name), key -> loader.get());
    }

    /** {@code paramTypes} is {@code null} for "resolve by name only" lookups (as opposed to a 0-length array). */
    public static Method method(Class<?> owner, String name, Class<?>[] paramTypes, Supplier<Method> loader) {
        List<Class<?>> key = paramTypes == null ? null : List.of(paramTypes);
        return METHODS.computeIfAbsent(new MethodKey(owner, name, key), k -> loader.get());
    }

    private record FieldKey(Class<?> owner, String name) {
    }

    private record MethodKey(Class<?> owner, String name, List<Class<?>> paramTypes) {
    }
}
