package io.axiom.json.internal.gson;

import com.google.gson.FieldNamingStrategy;
import io.axiom.json.annotations.JsonName;

import java.lang.reflect.Field;

final class AxiomFieldNamingStrategy implements FieldNamingStrategy {

    @Override
    public String translateName(Field field) {
        JsonName annotation = field.getAnnotation(JsonName.class);
        return annotation != null ? annotation.value() : field.getName();
    }
}
