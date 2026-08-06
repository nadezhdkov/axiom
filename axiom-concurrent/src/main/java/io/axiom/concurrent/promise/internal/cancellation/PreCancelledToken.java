package io.axiom.concurrent.promise.internal.cancellation;

import io.axiom.concurrent.promise.CancellationToken;
import io.axiom.concurrent.promise.error.CancellationException;

public record PreCancelledToken(String reason) implements CancellationToken {

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public void throwIfCancelled() {
        throw new CancellationException(reason);
    }

    @Override
    public void onCancelled(Runnable callback) {
        callback.run();
    }
}
