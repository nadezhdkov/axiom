package io.axiom.json.internal.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.axiom.json.annotations.JsonDefault;
import io.axiom.json.annotations.JsonName;
import io.axiom.json.annotations.JsonRequired;
import io.axiom.json.error.JsonValidationException;
import io.axiom.reflect.Reflect;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Enforces {@link JsonRequired}/{@link JsonDefault} during decode.
 *
 * <p>The Obsidian module this is ported from exposed the equivalent annotation-inspection hooks
 * ({@code GsonAnnotationProcessor#isRequired}/{@code #getDefaultValue}) but never wired them into
 * the actual Gson decode pipeline, so the documented "throws JsonValidationException" behavior of
 * {@code @JsonRequired} never actually fired. This factory closes that gap.
 */
final class AxiomTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        if (rawType.isInterface() || rawType.isEnum() || rawType.isPrimitive()
                || rawType.getName().startsWith("java.") || rawType.getName().startsWith("javax.")) {
            return null;
        }

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement element = elementAdapter.read(in);
                if (element != null && element.isJsonObject()) {
                    applyRequiredAndDefault(rawType, element.getAsJsonObject());
                }
                return delegate.fromJsonTree(element);
            }
        };
    }

    private static void applyRequiredAndDefault(Class<?> rawType, JsonObject object) {
        for (Field field : Reflect.on(rawType).fields().list()) {
            String serializedName = serializedName(field);
            boolean missing = !object.has(serializedName) || object.get(serializedName).isJsonNull();
            if (!missing) {
                continue;
            }
            JsonDefault defaultValue = field.getAnnotation(JsonDefault.class);
            if (defaultValue != null) {
                object.add(serializedName, defaultPrimitive(field.getType(), defaultValue.value()));
                continue;
            }
            if (field.isAnnotationPresent(JsonRequired.class)) {
                throw new JsonValidationException(
                        "Required field \"" + serializedName + "\" is missing or null for " + rawType.getName());
            }
        }
    }

    private static String serializedName(Field field) {
        JsonName name = field.getAnnotation(JsonName.class);
        return name != null ? name.value() : field.getName();
    }

    private static JsonElement defaultPrimitive(Class<?> fieldType, String value) {
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        }
        if (fieldType == int.class || fieldType == Integer.class || fieldType == long.class || fieldType == Long.class
                || fieldType == double.class || fieldType == Double.class || fieldType == float.class || fieldType == Float.class
                || fieldType == short.class || fieldType == Short.class || fieldType == byte.class || fieldType == Byte.class) {
            return new JsonPrimitive(new java.math.BigDecimal(value));
        }
        return new JsonPrimitive(value);
    }
}
