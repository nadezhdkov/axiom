package io.axiom.core.fn;

/**
 * A {@link Runnable}-like operation that is permitted to throw a checked exception.
 */
@FunctionalInterface
public interface FailableRunnable {
    void run() throws Exception;
}
