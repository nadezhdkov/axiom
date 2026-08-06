package io.axiom.concurrent.promise.error;

import java.time.Duration;

/**
 * Signals that a promise exceeded the allotted time limit ({@link io.axiom.concurrent.promise.Promise#timeout}).
 */
public class TimeoutException extends PromiseException {

    private final Duration timeout;

    public TimeoutException(Duration timeout) {
        super("Operation timed out after " + timeout);
        this.timeout = timeout;
    }

    public TimeoutException(String message, Duration timeout) {
        super(message);
        this.timeout = timeout;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
