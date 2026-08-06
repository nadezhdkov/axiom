package io.axiom.yaml.internal.snakeyaml;

import io.axiom.yaml.YamlMapping;
import io.axiom.yaml.YamlNode;
import io.axiom.yaml.YamlNull;
import io.axiom.yaml.YamlScalar;
import io.axiom.yaml.YamlSequence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The sole place SnakeYAML's raw object graph (nested {@code Map}/{@code List}/scalar, as
 * produced by {@code org.yaml.snakeyaml.Yaml#load}/consumed by {@code #dump}) crosses with the
 * {@code io.axiom.yaml.*} tree model.
 */
public final class RawBridge {

    private RawBridge() {
    }

    @SuppressWarnings("unchecked")
    public static YamlNode toAxiom(Object raw) {
        if (raw == null) {
            return YamlNull.INSTANCE;
        }
        if (raw instanceof Map<?, ?> map) {
            YamlMapping mapping = new YamlMapping();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                mapping.add(String.valueOf(entry.getKey()), toAxiom(entry.getValue()));
            }
            return mapping;
        }
        if (raw instanceof List<?> list) {
            YamlSequence sequence = new YamlSequence();
            for (Object element : list) {
                sequence.add(toAxiom(element));
            }
            return sequence;
        }
        if (raw instanceof String string) {
            return new YamlScalar(string);
        }
        if (raw instanceof Number number) {
            return new YamlScalar(number);
        }
        if (raw instanceof Boolean bool) {
            return new YamlScalar(bool);
        }
        return new YamlScalar(raw.toString());
    }

    public static Object toRaw(YamlNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node instanceof YamlMapping mapping) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, YamlNode> entry : mapping.entrySet()) {
                map.put(entry.getKey(), toRaw(entry.getValue()));
            }
            return map;
        }
        if (node instanceof YamlSequence sequence) {
            List<Object> list = new java.util.ArrayList<>();
            for (YamlNode element : sequence) {
                list.add(toRaw(element));
            }
            return list;
        }
        YamlScalar scalar = node.asScalar();
        if (scalar.isBoolean()) {
            return scalar.asBoolean();
        }
        if (scalar.isNumber()) {
            return scalar.asNumber();
        }
        return scalar.asString();
    }
}
