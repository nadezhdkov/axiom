package io.axiom.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as mandatory: decoding throws {@link io.axiom.json.error.JsonValidationException}
 * if the field is missing or {@code null} and no {@link JsonDefault} applies. Actually enforced
 * by {@code internal.gson.AxiomTypeAdapterFactory} — unlike the Obsidian original this annotation
 * was ported from, where the equivalent hook existed but was never wired into the decode path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonRequired {
}
