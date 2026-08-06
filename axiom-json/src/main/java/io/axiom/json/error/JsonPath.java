package io.axiom.json.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, persistent pointer to a location inside a JSON document (e.g. {@code $.a.b[0]}). */
public final class JsonPath {

    private static final JsonPath ROOT = new JsonPath(Collections.emptyList());

    private final List<String> segments;

    private JsonPath(List<String> segments) {
        this.segments = segments;
    }

    public static JsonPath root() {
        return ROOT;
    }

    public JsonPath field(String name) {
        List<String> next = new ArrayList<>(segments);
        next.add("." + name);
        return new JsonPath(Collections.unmodifiableList(next));
    }

    public JsonPath index(int i) {
        List<String> next = new ArrayList<>(segments);
        next.add("[" + i + "]");
        return new JsonPath(Collections.unmodifiableList(next));
    }

    public List<String> getSegments() {
        return segments;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public int size() {
        return segments.size();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsonPath other && segments.equals(other.segments);
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
