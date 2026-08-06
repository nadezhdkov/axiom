package io.axiom.json.io;

import io.axiom.json.error.JsonIoException;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A tagged union of the places JSON text can be written to: a {@link Writer} or a file. */
public final class JsonSink {

    private enum SinkType { WRITER, PATH }

    private final SinkType type;
    private final Writer writer;
    private final Path path;

    private JsonSink(SinkType type, Writer writer, Path path) {
        this.type = type;
        this.writer = writer;
        this.path = path;
    }

    public static JsonSink of(Writer writer) {
        Objects.requireNonNull(writer, "writer must not be null");
        return new JsonSink(SinkType.WRITER, writer, null);
    }

    public static JsonSink of(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return new JsonSink(SinkType.PATH, null, path);
    }

    public boolean isWriter() {
        return type == SinkType.WRITER;
    }

    public boolean isPath() {
        return type == SinkType.PATH;
    }

    public Writer asWriter() {
        return switch (type) {
            case WRITER -> writer;
            case PATH -> {
                try {
                    yield Files.newBufferedWriter(path, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new JsonIoException("Failed to open JSON sink: " + path, e);
                }
            }
        };
    }
}
