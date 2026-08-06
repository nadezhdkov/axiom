package io.axiom.json;

/** Singleton representing a JSON {@code null} literal. */
public final class JsonNull extends JsonElement {

    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    @Override
    public JsonElement deepCopy() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsonNull;
    }

    @Override
    public int hashCode() {
        return JsonNull.class.hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }
}
