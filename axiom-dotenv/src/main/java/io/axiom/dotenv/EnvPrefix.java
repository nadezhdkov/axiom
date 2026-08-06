package io.axiom.dotenv;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level prefix automatically prepended to every {@link Env} key in the class.
 *
 * <pre>{@code
 * @EnvPrefix("REDIS_")
 * class RedisConfig {
 *     @Env("HOST") String host; // REDIS_HOST
 * }
 * }</pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EnvPrefix {
    String value();
}
