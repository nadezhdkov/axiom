package io.axiom.core.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Captures a possibly-generic {@link Type} at runtime, following the standard "super type token"
 * idiom (create an anonymous subclass to preserve generics, e.g.
 * {@code new TypeReference<List<String>>() {}}).
 *
 * <p>Shared by any module that needs to decode into a generic target type without leaking a
 * third-party equivalent (e.g. Gson's own {@code TypeToken}) through its public API — currently
 * {@code axiom-json} and {@code axiom-yaml}, which independently reinvented the identical type
 * before this was pulled up into {@code axiom-core}.
 */
public abstract class TypeReference<T> {

    private final Type type;

    protected TypeReference() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType parameterized)) {
            throw new IllegalArgumentException("TypeReference constructed without actual type information");
        }
        this.type = parameterized.getActualTypeArguments()[0];
    }

    private TypeReference(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static <T> TypeReference<T> of(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return new TypeReference<>(type) {
        };
    }

    public static <E> TypeReference<List<E>> listOf(Class<E> elementType) {
        return new TypeReference<>(new ParameterizedTypeAdapter(List.class, elementType)) {
        };
    }

    public static <E> TypeReference<Set<E>> setOf(Class<E> elementType) {
        return new TypeReference<>(new ParameterizedTypeAdapter(Set.class, elementType)) {
        };
    }

    public static <K, V> TypeReference<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
        return new TypeReference<>(new ParameterizedTypeAdapter(Map.class, keyType, valueType)) {
        };
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeReference<?> other && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    private record ParameterizedTypeAdapter(Type rawType, Type... typeArguments) implements ParameterizedType {

        @Override
        public Type[] getActualTypeArguments() {
            return typeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }
}
