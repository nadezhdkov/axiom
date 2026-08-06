package io.axiom.concurrent.promise.internal.cancellation;

import io.axiom.concurrent.promise.CancellationSource;
import io.axiom.concurrent.promise.CancellationToken;

public class DefaultCancellationSource implements CancellationSource {

    private final DefaultCancellationToken token = new DefaultCancellationToken();

    @Override
    public CancellationToken token() {
        return token;
    }

    @Override
    public void cancel() {
        cancel("Cancelled");
    }

    @Override
    public void cancel(String reason) {
        token.cancel(reason);
    }

    @Override
    public boolean isCancelled() {
        return token.isCancelled();
    }
}
