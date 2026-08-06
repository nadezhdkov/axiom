package io.axiom.yaml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An insertion-ordered YAML mapping, with a dotted-path accessor
 * (e.g. {@code mapping.getPath("db.host")}) reimplemented over this own tree model — the API
 * shape is inspired by JToolBox's {@code YamlConfig}, but unlike it, this never navigates a raw
 * {@code Map<?,?>} by hand.
 */
public final class YamlMapping extends YamlNode {

    private final Map<String, YamlNode> members = new LinkedHashMap<>();

    public void add(String key, YamlNode value) {
        Objects.requireNonNull(key, "key must not be null");
        members.put(key, value == null ? YamlNull.INSTANCE : value);
    }

    public YamlNode remove(String key) {
        return members.remove(key);
    }

    public YamlNode get(String key) {
        return members.get(key);
    }

    public boolean has(String key) {
        return members.containsKey(key);
    }

    public Set<String> keySet() {
        return members.keySet();
    }

    public Set<Map.Entry<String, YamlNode>> entrySet() {
        return members.entrySet();
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    /** Resolves a dotted path (e.g. {@code "db.host"}) by walking nested {@link YamlMapping}s. */
    public YamlNode getPath(String dottedPath) {
        Objects.requireNonNull(dottedPath, "dottedPath must not be null");
        YamlNode current = this;
        for (String segment : dottedPath.split("\\.")) {
            if (!(current instanceof YamlMapping mapping) || !mapping.has(segment)) {
                return null;
            }
            current = mapping.get(segment);
        }
        return current;
    }

    public boolean containsPath(String dottedPath) {
        return getPath(dottedPath) != null;
    }

    public String getString(String dottedPath) {
        YamlNode node = getPath(dottedPath);
        return node == null || node.isNull() ? null : node.asScalar().asString();
    }

    public String getString(String dottedPath, String defaultValue) {
        String value = getString(dottedPath);
        return value != null ? value : defaultValue;
    }

    public int getInt(String dottedPath) {
        return getPath(dottedPath).asScalar().asInt();
    }

    public boolean getBoolean(String dottedPath) {
        return getPath(dottedPath).asScalar().asBoolean();
    }

    public double getDouble(String dottedPath) {
        return getPath(dottedPath).asScalar().asDouble();
    }

    @Override
    public YamlMapping deepCopy() {
        YamlMapping copy = new YamlMapping();
        for (Map.Entry<String, YamlNode> entry : members.entrySet()) {
            copy.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YamlMapping other && members.equals(other.members);
    }

    @Override
    public int hashCode() {
        return members.hashCode();
    }

    @Override
    public String toString() {
        return members.toString();
    }
}
