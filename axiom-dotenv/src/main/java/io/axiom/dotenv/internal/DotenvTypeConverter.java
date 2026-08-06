package io.axiom.dotenv.internal;

import io.axiom.dotenv.DotenvException;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Converts raw string {@code .env} values into typed field values. */
public final class DotenvTypeConverter {

    private static final Map<Class<?>, TypeConverter<?>> CONVERTERS = new HashMap<>();

    static {
        register(String.class, val -> val);
        register(Integer.class, Integer::valueOf);
        register(int.class, Integer::valueOf);
        register(Long.class, Long::valueOf);
        register(long.class, Long::valueOf);
        register(Double.class, Double::valueOf);
        register(double.class, Double::valueOf);
        register(Float.class, Float::valueOf);
        register(float.class, Float::valueOf);
        register(Boolean.class, Boolean::parseBoolean);
        register(boolean.class, Boolean::parseBoolean);
        register(Duration.class, Duration::parse);
        register(Path.class, Path::of);
        register(File.class, File::new);
    }

    private DotenvTypeConverter() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T convert(String val, Class<T> type) {
        if (val == null) return null;

        if (CONVERTERS.containsKey(type)) {
            return (T) CONVERTERS.get(type).convert(val);
        }
        if (type.isEnum()) {
            return (T) Enum.valueOf((Class<? extends Enum>) type, val.toUpperCase());
        }
        if (List.class.isAssignableFrom(type)) {
            return (T) toList(val);
        }
        if (Set.class.isAssignableFrom(type)) {
            return (T) new HashSet<>(toList(val));
        }
        throw new DotenvException("Unsupported @Env field type: " + type.getName());
    }

    public static <T> void register(Class<T> type, TypeConverter<T> converter) {
        CONVERTERS.put(type, converter);
    }

    private static List<String> toList(String val) {
        if (val == null || val.isBlank()) return Collections.emptyList();
        return Arrays.stream(val.split(",")).map(String::trim).collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface TypeConverter<T> {
        T convert(String value);
    }
}
