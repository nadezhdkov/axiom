package io.axiom.yaml.io;

import io.axiom.yaml.error.YamlIoException;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** A tagged union of the places YAML text can be written to: a {@link Writer} or a file. */
public final class YamlSink {

    private enum SinkType { WRITER, PATH }

    private final SinkType type;
    private final Writer writer;
    private final Path path;

    private YamlSink(SinkType type, Writer writer, Path path) {
        this.type = type;
        this.writer = writer;
        this.path = path;
    }

    public static YamlSink of(Writer writer) {
        Objects.requireNonNull(writer, "writer must not be null");
        return new YamlSink(SinkType.WRITER, writer, null);
    }

    public static YamlSink of(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        return new YamlSink(SinkType.PATH, null, path);
    }

    public Writer asWriter() {
        return switch (type) {
            case WRITER -> writer;
            case PATH -> {
                try {
                    yield Files.newBufferedWriter(path, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new YamlIoException("Failed to open YAML sink: " + path, e);
                }
            }
        };
    }
}
