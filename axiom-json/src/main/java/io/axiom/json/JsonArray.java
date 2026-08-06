package io.axiom.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** An ordered JSON array. */
public final class JsonArray extends JsonElement implements Iterable<JsonElement> {

    private final List<JsonElement> elements = new ArrayList<>();

    public void add(JsonElement element) {
        elements.add(element == null ? JsonNull.INSTANCE : element);
    }

    public void add(String value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void add(Number value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void add(Boolean value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void add(Character value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addAll(JsonArray other) {
        elements.addAll(other.elements);
    }

    public JsonElement set(int index, JsonElement element) {
        return elements.set(index, element == null ? JsonNull.INSTANCE : element);
    }

    public JsonElement remove(int index) {
        return elements.remove(index);
    }

    public boolean remove(JsonElement element) {
        return elements.remove(element);
    }

    public boolean contains(JsonElement element) {
        return elements.contains(element);
    }

    public JsonElement get(int index) {
        return elements.get(index);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return elements.iterator();
    }

    @Override
    public JsonArray deepCopy() {
        JsonArray copy = new JsonArray();
        for (JsonElement element : elements) {
            copy.add(element.deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsonArray other && elements.equals(other.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
