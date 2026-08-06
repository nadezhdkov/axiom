package io.axiom.json.internal.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import io.axiom.json.annotations.JsonIgnore;

final class AxiomExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes field) {
        return field.getAnnotation(JsonIgnore.class) != null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
