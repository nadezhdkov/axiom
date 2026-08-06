package io.axiom.concurrent.promise.error;

/**
 * Indicates that an asynchronous operation was intentionally cancelled before completion.
 *
 * <p>Named identically to {@link java.util.concurrent.CancellationException} by design (mirrors
 * the "cancellation" vocabulary of {@link io.axiom.concurrent.promise.Promise}); callers that need
 * both must fully qualify one of them.
 */
public class CancellationException extends PromiseException {

    public CancellationException() {
        super("Operation was cancelled");
    }

    public CancellationException(String reason) {
        super(reason != null ? reason : "Operation was cancelled");
    }

    public CancellationException(String message, Throwable cause) {
        super(message, cause);
    }
}
