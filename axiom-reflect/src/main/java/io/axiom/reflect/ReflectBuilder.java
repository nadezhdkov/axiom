package io.axiom.reflect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ReflectBuilder<T> {

    private final Class<T> type;
    private final Map<String, Object> values;

    private ReflectBuilder(Class<T> type) {
        this.type = type;
        this.values = new HashMap<>();
    }

    public static <T> ReflectBuilder<T> of(Class<T> type) {
        return new ReflectBuilder<>(type);
    }

    @SuppressWarnings("unchecked")
    public static <T> ReflectBuilder<T> from(T instance) {
        ReflectBuilder<T> builder = new ReflectBuilder<>((Class<T>) instance.getClass());

        Reflect.on(instance)
                .fields()
                .notStatic()
                .each(f -> {
                    try {
                        builder.set(f.getName(), f.get(instance));
                    } catch (Exception ignored) {
                    }
                });

        return builder;
    }

    public ReflectBuilder<T> set(String field, Object value) {
        values.put(field, value);
        return this;
    }

    public ReflectBuilder<T> setAll(Map<String, Object> values) {
        this.values.putAll(values);
        return this;
    }

    public ReflectBuilder<T> setIfNotNull(String field, Object value) {
        if (value != null) set(field, value);
        return this;
    }

    public ReflectBuilder<T> configure(Consumer<T> configurator) {
        T instance = build();
        configurator.accept(instance);
        return from(instance);
    }

    public ReflectBuilder<T> unset(String field) {
        values.remove(field);
        return this;
    }

    public ReflectBuilder<T> clear() {
        values.clear();
        return this;
    }

    public T build() {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            injectValues(instance);
            return instance;
        } catch (Exception e) {
            throw new ReflectException("Failed to build " + type.getName(), e);
        }
    }

    public T build(Object... args) {
        try {
            Class<?>[] types = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
            T instance = type.getDeclaredConstructor(types).newInstance(args);
            injectValues(instance);
            return instance;
        } catch (Exception e) {
            throw new ReflectException("Failed to build with args", e);
        }
    }

    public boolean has(String field) {
        return values.containsKey(field);
    }

    public Object get(String field) {
        return values.get(field);
    }

    public Map<String, Object> values() {
        return new HashMap<>(values);
    }

    private void injectValues(T instance) {
        Reflect reflect = Reflect.on(instance);
        values.forEach((name, val) -> {
            try {
                reflect.field(name).set(val);
            } catch (Exception e) {
                throw new ReflectException("Failed to set field: " + name, e);
            }
        });
    }
}
