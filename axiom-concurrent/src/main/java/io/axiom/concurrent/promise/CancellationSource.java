package io.axiom.concurrent.promise;

import io.axiom.concurrent.promise.internal.cancellation.DefaultCancellationSource;

/**
 * Controls a {@link CancellationToken}, allowing cancellation to be signaled.
 *
 * <pre>{@code
 * CancellationSource source = CancellationSource.create();
 * Promise<Data> promise = Promises.async(() -> loadData(source.token()));
 * source.cancel(); // later
 * }</pre>
 */
public interface CancellationSource {

    CancellationToken token();

    void cancel();

    void cancel(String reason);

    boolean isCancelled();

    static CancellationSource create() {
        return new DefaultCancellationSource();
    }

    /** Cancelled when any of the given parent tokens is cancelled. */
    static CancellationSource createLinked(CancellationToken... tokens) {
        DefaultCancellationSource source = new DefaultCancellationSource();
        for (CancellationToken token : tokens) {
            token.onCancelled(() -> source.cancel("Linked cancellation"));
        }
        return source;
    }
}
