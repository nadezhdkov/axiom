package io.axiom.json.internal.gson;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.axiom.json.JsonConfig;
import io.axiom.json.JsonElement;
import io.axiom.json.JsonMapper;
import io.axiom.core.type.TypeReference;
import io.axiom.json.error.JsonIoException;
import io.axiom.json.error.JsonMappingException;
import io.axiom.json.error.JsonParseException;
import io.axiom.json.io.JsonSource;
import io.axiom.json.util.JsonPrettyPrinter;

import java.io.IOException;
import java.io.Reader;

public final class GsonMapper implements JsonMapper {

    private final Gson gson;
    private final JsonConfig config;

    public GsonMapper(JsonConfig config) {
        this.config = config;
        this.gson = new GsonEngine(config).createGson();
    }

    @Override
    public JsonElement parse(JsonSource source) {
        try (Reader reader = source.asReader()) {
            com.google.gson.JsonElement element = gson.fromJson(reader, com.google.gson.JsonElement.class);
            return GsonElementBridge.toAxiom(element);
        } catch (JsonSyntaxException e) {
            throw new JsonParseException("Failed to parse JSON from " + source, e);
        } catch (IOException e) {
            throw new JsonIoException("Failed to read JSON from " + source, e);
        }
    }

    @Override
    public <T> T decode(JsonSource source, TypeReference<T> type) {
        try (Reader reader = source.asReader()) {
            return gson.fromJson(reader, type.getType());
        } catch (JsonSyntaxException e) {
            throw new JsonMappingException("Failed to decode " + type + " from " + source, e);
        } catch (IOException e) {
            throw new JsonIoException("Failed to read JSON from " + source, e);
        }
    }

    @Override
    public <T> T decode(JsonElement element, TypeReference<T> type) {
        try {
            return gson.fromJson(GsonElementBridge.toGson(element), type.getType());
        } catch (JsonSyntaxException e) {
            throw new JsonMappingException("Failed to decode " + type + " from " + element, e);
        }
    }

    @Override
    public JsonElement encode(Object value) {
        try {
            return GsonElementBridge.toAxiom(gson.toJsonTree(value));
        } catch (JsonIOException e) {
            throw new JsonMappingException("Failed to encode value of type "
                    + (value == null ? "null" : value.getClass()), e);
        }
    }

    @Override
    public String stringify(JsonElement element) {
        return config.isPrettyPrint()
                ? JsonPrettyPrinter.toPrettyString(element)
                : JsonPrettyPrinter.toCompactString(element);
    }
}
