package io.axiom.yaml;

import io.axiom.core.type.TypeReference;
import io.axiom.yaml.io.YamlSource;

/** Thread-safe conversion between YAML text, the {@link YamlNode} tree, and Java objects. */
public interface YamlMapper {

    YamlNode parse(YamlSource source);

    <T> T decode(YamlSource source, TypeReference<T> type);

    <T> T decode(YamlNode node, TypeReference<T> type);

    YamlNode encode(Object value);

    String stringify(YamlNode node);

    default <T> T decode(YamlSource source, Class<T> type) {
        return decode(source, TypeReference.of(type));
    }

    default <T> T decode(YamlNode node, Class<T> type) {
        return decode(node, TypeReference.of(type));
    }

    default String toYaml(Object value) {
        return stringify(encode(value));
    }
}
