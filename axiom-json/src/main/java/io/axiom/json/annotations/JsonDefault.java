package io.axiom.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies a default value, as string form, applied when the field is absent or {@code null}
 * during decoding. Actually enforced by {@code internal.gson.AxiomTypeAdapterFactory} — unlike
 * the Obsidian original this annotation was ported from, where the equivalent hook existed but
 * was never wired into the decode path.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonDefault {
    String value();
}
