package io.axiom.yaml.io;

import io.axiom.yaml.error.YamlIoException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A tagged union of the places YAML text can be read from: a string, a {@link Reader}, or a file. */
public final class YamlSource {

    private enum SourceType { STRING, READER, PATH }

    private final SourceType type;
    private final String string;
    private final Reader reader;
    private final Path path;

    private YamlSource(SourceType type, String string, Reader reader, Path path) {
        this.type = type;
        this.string = string;
        this.reader = reader;
        this.path = path;
    }

    public static YamlSource of(String yaml) {
        Objects.requireNonNull(yaml, "yaml must not be null");
        return new YamlSource(SourceType.STRING, yaml, null, null);
    }

    public static YamlSource of(Reader reader) {
        Objects.requireNonNull(reader, "reader must not be null");
        return new YamlSource(SourceType.READER, null, reader, null);
    }

    public static YamlSource of(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return new YamlSource(SourceType.PATH, null, null, path);
    }

    public Reader asReader() {
        return switch (type) {
            case STRING -> new StringReader(string);
            case READER -> reader;
            case PATH -> {
                try {
                    yield Files.newBufferedReader(path, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new YamlIoException("Failed to open YAML source: " + path, e);
                }
            }
        };
    }

    @Override
    public String toString() {
        return switch (type) {
            case STRING -> "YamlSource[string=" + truncate(string) + "]";
            case READER -> "YamlSource[reader=" + reader + "]";
            case PATH -> "YamlSource[path=" + path + "]";
        };
    }

    private static String truncate(String value) {
        return value.length() > 50 ? value.substring(0, 50) + "..." : value;
    }
}
