package io.axiom.json;

/**
 * Base of the Axiom JSON tree model.
 *
 * <p>This type never touches the underlying parsing/serialization engine — {@code axiom-json}
 * hides Gson (or any future engine) behind {@code internal.gson.*}, so nothing in this public
 * API leaks a third-party type.
 */
public abstract sealed class JsonElement permits JsonObject, JsonArray, JsonPrimitive, JsonNull {

    public boolean isJsonObject() {
        return this instanceof JsonObject;
    }

    public boolean isJsonArray() {
        return this instanceof JsonArray;
    }

    public boolean isJsonPrimitive() {
        return this instanceof JsonPrimitive;
    }

    public boolean isJsonNull() {
        return this instanceof JsonNull;
    }

    public JsonObject asJsonObject() {
        if (this instanceof JsonObject object) {
            return object;
        }
        throw new IllegalStateException("Not a JsonObject: " + this);
    }

    public JsonArray asJsonArray() {
        if (this instanceof JsonArray array) {
            return array;
        }
        throw new IllegalStateException("Not a JsonArray: " + this);
    }

    public JsonPrimitive asJsonPrimitive() {
        if (this instanceof JsonPrimitive primitive) {
            return primitive;
        }
        throw new IllegalStateException("Not a JsonPrimitive: " + this);
    }

    public abstract JsonElement deepCopy();
}
