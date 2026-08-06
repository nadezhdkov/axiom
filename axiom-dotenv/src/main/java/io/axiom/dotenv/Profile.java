package io.axiom.dotenv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts {@link DotenvBinder#bind} to only apply when the {@link Dotenv}'s
 * {@link Dotenv#activeProfile()} matches one of the listed profiles.
 *
 * <p>Pairs with {@link DotenvBuilder#profile(String)}, which also causes a sibling
 * {@code <filename>.<profile>} file to be loaded and merged on top of the base file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Profile {
    String[] value();
}
