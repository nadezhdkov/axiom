package io.axiom.json;

import io.axiom.core.type.TypeReference;
import io.axiom.json.io.JsonSource;

/** Thread-safe conversion between JSON text, the {@link JsonElement} tree, and Java objects. */
public interface JsonMapper {

    JsonElement parse(JsonSource source);

    <T> T decode(JsonSource source, TypeReference<T> type);

    <T> T decode(JsonElement element, TypeReference<T> type);

    JsonElement encode(Object value);

    String stringify(JsonElement element);

    default <T> T decode(JsonSource source, Class<T> type) {
        return decode(source, TypeReference.of(type));
    }

    default <T> T decode(JsonElement element, Class<T> type) {
        return decode(element, TypeReference.of(type));
    }

    default String toJson(Object value) {
        return stringify(encode(value));
    }
}
