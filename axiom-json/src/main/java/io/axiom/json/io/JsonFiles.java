package io.axiom.json.io;

import io.axiom.json.Json;
import io.axiom.json.JsonElement;
import io.axiom.json.JsonMapper;
import io.axiom.core.type.TypeReference;
import io.axiom.json.error.JsonIoException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Convenience static entry points for reading/writing JSON directly against {@link Path}s. */
public final class JsonFiles {

    private JsonFiles() {
    }

    public static <T> T read(Path path, TypeReference<T> type) {
        return read(path, type, Json.defaultMapper());
    }

    public static <T> T read(Path path, TypeReference<T> type, JsonMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return mapper.decode(JsonSource.of(path), type);
    }

    public static JsonElement parse(Path path) {
        return parse(path, Json.defaultMapper());
    }

    public static JsonElement parse(Path path, JsonMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return mapper.parse(JsonSource.of(path));
    }

    public static void write(Path path, Object value) {
        write(path, value, Json.defaultMapper());
    }

    public static void write(Path path, Object value, JsonMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        try {
            Files.writeString(path, mapper.toJson(value), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JsonIoException("Failed to write JSON to " + path, e);
        }
    }

    public static void write(Path path, JsonElement element) {
        write(path, element, Json.defaultMapper());
    }

    public static void write(Path path, JsonElement element, JsonMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        try {
            Files.writeString(path, mapper.stringify(element), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new JsonIoException("Failed to write JSON to " + path, e);
        }
    }
}
