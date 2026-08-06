package io.axiom.yaml.io;

import io.axiom.yaml.Yaml;
import io.axiom.yaml.YamlMapper;
import io.axiom.yaml.YamlNode;
import io.axiom.core.type.TypeReference;
import io.axiom.yaml.error.YamlIoException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Convenience static entry points for reading/writing YAML directly against {@link Path}s. */
public final class YamlFiles {

    private YamlFiles() {
    }

    public static <T> T read(Path path, TypeReference<T> type) {
        return read(path, type, Yaml.defaultMapper());
    }

    public static <T> T read(Path path, TypeReference<T> type, YamlMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return mapper.decode(YamlSource.of(path), type);
    }

    public static YamlNode parse(Path path) {
        return parse(path, Yaml.defaultMapper());
    }

    public static YamlNode parse(Path path, YamlMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return mapper.parse(YamlSource.of(path));
    }

    public static void write(Path path, Object value) {
        write(path, value, Yaml.defaultMapper());
    }

    public static void write(Path path, Object value, YamlMapper mapper) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        try {
            Files.writeString(path, mapper.toYaml(value), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new YamlIoException("Failed to write YAML to " + path, e);
        }
    }
}
