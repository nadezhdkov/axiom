package io.axiom.concurrent.promise.internal.cancellation;

import io.axiom.concurrent.promise.CancellationToken;
import io.axiom.concurrent.promise.error.CancellationException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultCancellationToken implements CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<String> reason = new AtomicReference<>();
    private final List<Runnable> callbacks = new CopyOnWriteArrayList<>();

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public String reason() {
        return reason.get();
    }

    @Override
    public void throwIfCancelled() {
        if (cancelled.get()) {
            throw new CancellationException(reason.get());
        }
    }

    @Override
    public void onCancelled(Runnable callback) {
        if (cancelled.get()) {
            callback.run();
            return;
        }
        callbacks.add(callback);
        if (cancelled.get()) {
            callback.run();
        }
    }

    public void cancel(String cancelReason) {
        if (cancelled.compareAndSet(false, true)) {
            reason.set(cancelReason);
            callbacks.forEach(Runnable::run);
            callbacks.clear();
        }
    }
}
