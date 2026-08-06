package io.axiom.yaml;

import io.axiom.yaml.internal.snakeyaml.SnakeYamlMapper;

/** Entry point for {@code axiom-yaml}; never exposes the underlying engine (SnakeYAML) publicly. */
public final class Yaml {

    private static final YamlMapper DEFAULT_MAPPER = new SnakeYamlMapper(YamlConfig.defaultConfig());

    private Yaml() {
        throw new UnsupportedOperationException("Yaml is a static utility class");
    }

    public static YamlMapper defaultMapper() {
        return DEFAULT_MAPPER;
    }

    public static YamlConfig.Builder configure() {
        return YamlConfig.builder();
    }

    public static YamlMapper mapper(YamlConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return new SnakeYamlMapper(config);
    }
}
