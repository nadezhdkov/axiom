package io.axiom.json;

import io.axiom.json.internal.gson.GsonMapper;

/** Entry point for {@code axiom-json}; never exposes the underlying engine (Gson) publicly. */
public final class Json {

    private static final JsonMapper DEFAULT_MAPPER = new GsonMapper(JsonConfig.defaultConfig());

    private Json() {
        throw new UnsupportedOperationException("Json is a static utility class");
    }

    public static JsonMapper defaultMapper() {
        return DEFAULT_MAPPER;
    }

    public static JsonConfig.Builder configure() {
        return JsonConfig.builder();
    }

    public static JsonMapper mapper(JsonConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return new GsonMapper(config);
    }
}
