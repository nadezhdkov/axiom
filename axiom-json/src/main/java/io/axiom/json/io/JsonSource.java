package io.axiom.json.io;

import io.axiom.json.error.JsonIoException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A tagged union of the places JSON text can be read from: a string, a {@link Reader}, or a file. */
public final class JsonSource {

    private enum SourceType { STRING, READER, PATH }

    private final SourceType type;
    private final String string;
    private final Reader reader;
    private final Path path;

    private JsonSource(SourceType type, String string, Reader reader, Path path) {
        this.type = type;
        this.string = string;
        this.reader = reader;
        this.path = path;
    }

    public static JsonSource of(String json) {
        Objects.requireNonNull(json, "json must not be null");
        return new JsonSource(SourceType.STRING, json, null, null);
    }

    public static JsonSource of(Reader reader) {
        Objects.requireNonNull(reader, "reader must not be null");
        return new JsonSource(SourceType.READER, null, reader, null);
    }

    public static JsonSource of(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return new JsonSource(SourceType.PATH, null, null, path);
    }

    public boolean isString() {
        return type == SourceType.STRING;
    }

    public boolean isReader() {
        return type == SourceType.READER;
    }

    public boolean isPath() {
        return type == SourceType.PATH;
    }

    public Reader asReader() {
        return switch (type) {
            case STRING -> new StringReader(string);
            case READER -> reader;
            case PATH -> {
                try {
                    yield Files.newBufferedReader(path, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new JsonIoException("Failed to open JSON source: " + path, e);
                }
            }
        };
    }

    @Override
    public String toString() {
        return switch (type) {
            case STRING -> "JsonSource[string=" + truncate(string) + "]";
            case READER -> "JsonSource[reader=" + reader + "]";
            case PATH -> "JsonSource[path=" + path + "]";
        };
    }

    private static String truncate(String value) {
        return value.length() > 50 ? value.substring(0, 50) + "..." : value;
    }
}
