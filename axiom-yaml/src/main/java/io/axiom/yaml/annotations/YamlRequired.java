package io.axiom.yaml.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as mandatory: decoding throws {@link io.axiom.yaml.error.YamlValidationException}
 * if the key is missing or {@code null} and no {@link YamlDefault} applies.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface YamlRequired {
}
