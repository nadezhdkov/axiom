package io.axiom.concurrent.promise.internal.cancellation;

import io.axiom.concurrent.promise.CancellationToken;

public final class NoCancellationToken implements CancellationToken {

    private static final NoCancellationToken INSTANCE = new NoCancellationToken();

    public static NoCancellationToken getInstance() {
        return INSTANCE;
    }

    private NoCancellationToken() {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public String reason() {
        return null;
    }

    @Override
    public void throwIfCancelled() {
        // never thrown
    }

    @Override
    public void onCancelled(Runnable callback) {
        // never invoked
    }
}
