package io.axiom.yaml.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, persistent pointer to a location inside a YAML document. */
public final class YamlPath {

    private static final YamlPath ROOT = new YamlPath(Collections.emptyList());

    private final List<String> segments;

    private YamlPath(List<String> segments) {
        this.segments = segments;
    }

    public static YamlPath root() {
        return ROOT;
    }

    public YamlPath field(String name) {
        List<String> next = new ArrayList<>(segments);
        next.add("." + name);
        return new YamlPath(Collections.unmodifiableList(next));
    }

    public YamlPath index(int i) {
        List<String> next = new ArrayList<>(segments);
        next.add("[" + i + "]");
        return new YamlPath(Collections.unmodifiableList(next));
    }

    public List<String> getSegments() {
        return segments;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof YamlPath other && segments.equals(other.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("$");
        for (String segment : segments) {
            builder.append(segment);
        }
        return builder.toString();
    }
}
