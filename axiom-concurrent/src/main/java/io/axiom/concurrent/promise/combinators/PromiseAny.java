package io.axiom.concurrent.promise.combinators;

import io.axiom.concurrent.promise.Deferred;
import io.axiom.concurrent.promise.Promise;
import io.axiom.concurrent.promise.Promises;
import io.axiom.concurrent.promise.error.AggregateException;
import io.axiom.concurrent.promise.error.CancellationException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resolves with the first promise that succeeds; rejects with an {@link AggregateException} only
 * if every promise fails.
 */
public final class PromiseAny {

    private PromiseAny() {
    }

    public static <T> Promise<T> of(List<Promise<? extends T>> promises) {
        if (promises == null || promises.isEmpty()) {
            return Promises.error(new IllegalArgumentException("At least one promise is required"));
        }
        if (promises.size() == 1) {
            @SuppressWarnings("unchecked")
            Promise<T> single = (Promise<T>) promises.get(0);
            return single;
        }
        return new PromiseAnyImpl<>(promises).execute();
    }

    private static final class PromiseAnyImpl<T> {
        private final List<Promise<? extends T>> promises;
        private final Deferred<T> deferred;
        private final AtomicBoolean resolved;
        private final AtomicInteger failedCount;
        private final List<Throwable> errors;

        PromiseAnyImpl(List<Promise<? extends T>> promises) {
            this.promises = promises;
            this.deferred = Promises.defer();
            this.resolved = new AtomicBoolean(false);
            this.failedCount = new AtomicInteger(0);
            this.errors = new ArrayList<>();
        }

        Promise<T> execute() {
            for (Promise<? extends T> promise : promises) {
                promise.onSuccess(this::onSuccess).onError(this::onError).onCancelled(this::onCancelled);
            }
            return deferred.promise();
        }

        private void onSuccess(T result) {
            if (resolved.compareAndSet(false, true)) {
                deferred.resolve(result);
                cancelRemaining();
            }
        }

        private void onError(Throwable error) {
            synchronized (errors) {
                errors.add(error);
            }
            if (failedCount.incrementAndGet() == promises.size()) {
                if (resolved.compareAndSet(false, true)) {
                    synchronized (errors) {
                        deferred.reject(new AggregateException("All promises failed", errors));
                    }
                }
            }
        }

        private void onCancelled() {
            onError(new CancellationException());
        }

        private void cancelRemaining() {
            for (Promise<? extends T> promise : promises) {
                if (promise.isPending()) promise.cancel();
            }
        }
    }
}
