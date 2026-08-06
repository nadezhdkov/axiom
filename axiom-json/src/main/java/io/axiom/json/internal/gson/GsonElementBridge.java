package io.axiom.json.internal.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

/** The sole place {@code com.google.gson.*} types cross with {@code io.axiom.json.*} types. */
public final class GsonElementBridge {

    private GsonElementBridge() {
    }

    public static io.axiom.json.JsonElement toAxiom(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return io.axiom.json.JsonNull.INSTANCE;
        }
        if (element.isJsonPrimitive()) {
            return toAxiomPrimitive(element.getAsJsonPrimitive());
        }
        if (element.isJsonObject()) {
            return toAxiomObject(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            return toAxiomArray(element.getAsJsonArray());
        }
        throw new IllegalArgumentException("Unknown Gson element type: " + element.getClass());
    }

    public static JsonElement toGson(io.axiom.json.JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonPrimitive()) {
            return toGsonPrimitive(element.asJsonPrimitive());
        }
        if (element.isJsonObject()) {
            return toGsonObject(element.asJsonObject());
        }
        if (element.isJsonArray()) {
            return toGsonArray(element.asJsonArray());
        }
        throw new IllegalArgumentException("Unknown Axiom element type: " + element.getClass());
    }

    private static io.axiom.json.JsonElement toAxiomPrimitive(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return new io.axiom.json.JsonPrimitive(primitive.getAsBoolean());
        }
        if (primitive.isNumber()) {
            return new io.axiom.json.JsonPrimitive(primitive.getAsNumber());
        }
        return new io.axiom.json.JsonPrimitive(primitive.getAsString());
    }

    private static io.axiom.json.JsonObject toAxiomObject(JsonObject object) {
        io.axiom.json.JsonObject result = new io.axiom.json.JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.add(entry.getKey(), toAxiom(entry.getValue()));
        }
        return result;
    }

    private static io.axiom.json.JsonArray toAxiomArray(JsonArray array) {
        io.axiom.json.JsonArray result = new io.axiom.json.JsonArray();
        for (JsonElement element : array) {
            result.add(toAxiom(element));
        }
        return result;
    }

    private static JsonElement toGsonPrimitive(io.axiom.json.JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return new JsonPrimitive(primitive.getAsBoolean());
        }
        if (primitive.isNumber()) {
            return new JsonPrimitive(primitive.getAsNumber());
        }
        return new JsonPrimitive(primitive.asString());
    }

    private static JsonObject toGsonObject(io.axiom.json.JsonObject object) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, io.axiom.json.JsonElement> entry : object.entrySet()) {
            result.add(entry.getKey(), toGson(entry.getValue()));
        }
        return result;
    }

    private static JsonArray toGsonArray(io.axiom.json.JsonArray array) {
        JsonArray result = new JsonArray();
        for (io.axiom.json.JsonElement element : array) {
            result.add(toGson(element));
        }
        return result;
    }
}
