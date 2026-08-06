package io.axiom.json.internal.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.axiom.json.JsonConfig;

public final class GsonEngine {

    private final JsonConfig config;

    public GsonEngine(JsonConfig config) {
        this.config = config;
    }

    public Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        if (config.isPrettyPrint()) {
            builder.setPrettyPrinting();
        }
        if (config.isSerializeNulls()) {
            builder.serializeNulls();
        }
        if (config.isLenient()) {
            builder.setLenient();
        }
        if (!config.isHtmlEscaping()) {
            builder.disableHtmlEscaping();
        }
        if (config.getDateFormat() != null) {
            builder.setDateFormat(config.getDateFormat());
        }
        if (config.isAnnotationsEnabled() && config.getAnnotationsMode() == JsonConfig.AnnotationsMode.AXIOM_ONLY) {
            builder.setFieldNamingStrategy(new AxiomFieldNamingStrategy());
            builder.addSerializationExclusionStrategy(new AxiomExclusionStrategy());
            builder.addDeserializationExclusionStrategy(new AxiomExclusionStrategy());
            builder.registerTypeAdapterFactory(new AxiomTypeAdapterFactory());
        }
        return builder.create();
    }
}
