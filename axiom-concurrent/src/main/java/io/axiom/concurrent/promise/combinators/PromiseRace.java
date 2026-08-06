package io.axiom.concurrent.promise.combinators;

import io.axiom.concurrent.promise.Deferred;
import io.axiom.concurrent.promise.Promise;
import io.axiom.concurrent.promise.Promises;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Completes with the first promise to complete, success or failure, unlike {@link PromiseAny}. */
public final class PromiseRace {

    private PromiseRace() {
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
        return new PromiseRacer<>(promises).execute();
    }

    private static final class PromiseRacer<T> {
        private final List<Promise<? extends T>> promises;
        private final Deferred<T> deferred;
        private final AtomicBoolean completed;

        PromiseRacer(List<Promise<? extends T>> promises) {
            this.promises = promises;
            this.deferred = Promises.defer();
            this.completed = new AtomicBoolean(false);
        }

        Promise<T> execute() {
            for (Promise<? extends T> promise : promises) {
                promise.onSuccess(this::onSuccess).onError(this::onError).onCancelled(this::onCancelled);
            }
            return deferred.promise();
        }

        private void onSuccess(T result) {
            if (completed.compareAndSet(false, true)) {
                deferred.resolve(result);
                cancelRemaining();
            }
        }

        private void onError(Throwable error) {
            if (completed.compareAndSet(false, true)) {
                deferred.reject(error);
                cancelRemaining();
            }
        }

        private void onCancelled() {
            if (completed.compareAndSet(false, true)) {
                deferred.cancel("First promise was cancelled");
                cancelRemaining();
            }
        }

        private void cancelRemaining() {
            for (Promise<? extends T> promise : promises) {
                if (promise.isPending()) promise.cancel();
            }
        }
    }
}
