package io.axiom.dotenv;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for injection by {@link DotenvBinder}, naming the environment variable to read.
 *
 * <pre>{@code
 * @Env("DB_HOST")
 * String host;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Env {
    String value();
}
