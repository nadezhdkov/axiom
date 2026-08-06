package io.axiom.json.annotations;

import io.axiom.json.codec.JsonCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Attaches a custom {@link JsonCodec} to a field, instantiated reflectively via a no-arg constructor. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonAdapter {
    Class<? extends JsonCodec<?>> value();
}
