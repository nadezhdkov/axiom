package io.axiom.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** An insertion-ordered JSON object. */
public final class JsonObject extends JsonElement {

    private final Map<String, JsonElement> members = new LinkedHashMap<>();

    public void add(String property, JsonElement value) {
        Objects.requireNonNull(property, "property must not be null");
        members.put(property, value == null ? JsonNull.INSTANCE : value);
    }

    public void addProperty(String property, String value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Number value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Boolean value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Character value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public JsonElement remove(String property) {
        return members.remove(property);
    }

    public JsonElement get(String property) {
        return members.get(property);
    }

    public JsonObject getAsJsonObject(String property) {
        JsonElement element = members.get(property);
        return element == null ? null : element.asJsonObject();
    }

    public JsonArray getAsJsonArray(String property) {
        JsonElement element = members.get(property);
        return element == null ? null : element.asJsonArray();
    }

    public JsonPrimitive getAsJsonPrimitive(String property) {
        JsonElement element = members.get(property);
        return element == null ? null : element.asJsonPrimitive();
    }

    public boolean has(String property) {
        return members.containsKey(property);
    }

    public Set<String> keySet() {
        return members.keySet();
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return members.entrySet();
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override
    public JsonObject deepCopy() {
        JsonObject copy = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
            copy.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsonObject other && members.equals(other.members);
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
