package io.axiom.concurrent.promise;

import io.axiom.concurrent.promise.internal.cancellation.NoCancellationToken;
import io.axiom.concurrent.promise.internal.cancellation.PreCancelledToken;

/**
 * A read-only view of cancellation state for a cooperatively cancellable operation. To create
 * and control a token, use {@link CancellationSource}.
 *
 * <pre>{@code
 * void performWork(CancellationToken token) {
 *     while (!token.isCancelled()) {
 *         token.throwIfCancelled();
 *         // do work...
 *     }
 * }
 * }</pre>
 */
public interface CancellationToken {

    boolean isCancelled();

    String reason();

    /** @throws io.axiom.concurrent.promise.error.CancellationException if cancelled */
    void throwIfCancelled();

    /** If cancellation was already requested, the callback runs immediately. */
    void onCancelled(Runnable callback);

    static CancellationToken none() {
        return NoCancellationToken.getInstance();
    }

    static CancellationToken cancelled() {
        return cancelled("Already cancelled");
    }

    static CancellationToken cancelled(String reason) {
        return new PreCancelledToken(reason);
    }
}
