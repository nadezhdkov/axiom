package io.axiom.concurrent.promise;

/**
 * The producer side of a {@link Promise}: manual control over completion (resolve/reject/cancel).
 * The associated {@link #promise()} is the consumer side, to be handed to callers who should
 * only observe the result.
 *
 * <pre>{@code
 * Deferred<String> deferred = Promises.defer();
 * Promise<String> promise = deferred.promise(); // give this to consumers
 *
 * if (success) deferred.resolve("result");
 * else deferred.reject(new Exception("failed"));
 * }</pre>
 *
 * @param <T> the type of value this Deferred will produce
 */
public interface Deferred<T> {

    Promise<T> promise();

    /** @return true if this call completed the Promise, false if it was already completed */
    boolean resolve(T value);

    boolean reject(Throwable error);

    boolean cancel();

    boolean cancel(String reason);

    boolean isCompleted();

    /** Resolves/rejects/cancels this Deferred to mirror the given Promise's outcome. */
    void completeWith(Promise<? extends T> promise);
}
