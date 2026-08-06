package io.axiom.dotenv;

/** Thrown when {@code @Env} field injection fails: missing required value or type conversion error. */
public class DotenvInjectionException extends DotenvException {

    public DotenvInjectionException(String message) {
        super(message);
    }

    public DotenvInjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
