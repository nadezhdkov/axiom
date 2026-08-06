package io.axiom.concurrent.promise.error;

/**
 * Root unchecked exception for errors specific to promise execution.
 */
public class PromiseException extends RuntimeException {

    public PromiseException() {
        super();
    }

    public PromiseException(String message) {
        super(message);
    }

    public PromiseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PromiseException(Throwable cause) {
        super(cause);
    }
}
