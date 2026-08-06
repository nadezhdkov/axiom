package io.axiom.concurrent.promise.internal;

import io.axiom.concurrent.promise.Deferred;
import io.axiom.concurrent.promise.Promise;

import java.util.concurrent.CompletableFuture;

public class DefaultDeferred<T> implements Deferred<T> {

    private final CompletableFuture<T> future;
    private final DefaultPromise<T> promise;

    public DefaultDeferred() {
        this.future = new CompletableFuture<>();
        this.promise = DefaultPromise.fromCompletableFuture(future);
    }

    @Override
    public Promise<T> promise() {
        return promise;
    }

    @Override
    public boolean resolve(T value) {
        return future.complete(value);
    }

    @Override
    public boolean reject(Throwable error) {
        return future.completeExceptionally(error);
    }

    @Override
    public boolean cancel() {
        return future.cancel(true);
    }

    @Override
    public boolean cancel(String reason) {
        return future.completeExceptionally(new io.axiom.concurrent.promise.error.CancellationException(reason));
    }

    @Override
    public boolean isCompleted() {
        return future.isDone();
    }

    @Override
    public void completeWith(Promise<? extends T> otherPromise) {
        otherPromise.onSuccess(this::resolve).onError(this::reject).onCancelled(this::cancel);
    }
}
