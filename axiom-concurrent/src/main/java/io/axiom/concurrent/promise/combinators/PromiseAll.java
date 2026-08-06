package io.axiom.concurrent.promise.combinators;

import io.axiom.concurrent.promise.Deferred;
import io.axiom.concurrent.promise.Promise;
import io.axiom.concurrent.promise.Promises;
import io.axiom.concurrent.promise.error.AggregateException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Combines multiple promises into one that resolves when all input promises succeed, or rejects
 * fail-fast with the first error encountered.
 */
public final class PromiseAll {

    private PromiseAll() {
    }

    public static <T> Promise<List<T>> of(List<Promise<? extends T>> promises) {
        if (promises == null || promises.isEmpty()) {
            return Promises.value(new ArrayList<>());
        }
        if (promises.size() == 1) {
            return promises.get(0).map(result -> {
                List<T> list = new ArrayList<>();
                list.add(result);
                return list;
            });
        }
        return new PromiseCollector<>(promises).execute();
    }

    private static final class PromiseCollector<T> {
        private final List<Promise<? extends T>> promises;
        private final Deferred<List<T>> deferred;
        private final AtomicInteger remaining;
        private final AtomicReference<List<T>> results;
        private final AtomicReference<Throwable> firstError;

        PromiseCollector(List<Promise<? extends T>> promises) {
            this.promises = promises;
            this.deferred = Promises.defer();
            this.remaining = new AtomicInteger(promises.size());
            this.results = new AtomicReference<>(createResultsList(promises.size()));
            this.firstError = new AtomicReference<>();
        }

        Promise<List<T>> execute() {
            for (int i = 0; i < promises.size(); i++) {
                int index = i;
                promises.get(i)
                        .onSuccess(result -> onSuccess(index, result))
                        .onError(this::onError)
                        .onCancelled(this::onCancelled);
            }
            return deferred.promise();
        }

        private void onSuccess(int index, T result) {
            results.get().set(index, result);
            if (remaining.decrementAndGet() == 0) {
                deferred.resolve(results.get());
            }
        }

        private void onError(Throwable error) {
            if (firstError.compareAndSet(null, error)) {
                deferred.reject(error);
                cancelRemaining();
            }
        }

        private void onCancelled() {
            if (firstError.compareAndSet(null, new io.axiom.concurrent.promise.error.CancellationException())) {
                deferred.cancel("One of the promises was cancelled");
                cancelRemaining();
            }
        }

        private void cancelRemaining() {
            for (Promise<? extends T> promise : promises) {
                if (promise.isPending()) promise.cancel();
            }
        }

        private static <T> List<T> createResultsList(int size) {
            List<T> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(null);
            return list;
        }
    }

    /** Like {@link #of}, but collects every error into an {@link AggregateException} instead of failing fast. */
    public static <T> Promise<List<T>> aggregateResults(List<Promise<? extends T>> promises) {
        if (promises == null || promises.isEmpty()) {
            return Promises.value(new ArrayList<>());
        }
        return new PromiseAggregator<>(promises).execute();
    }

    private static final class PromiseAggregator<T> {
        private final List<Promise<? extends T>> promises;
        private final Deferred<List<T>> deferred;
        private final AtomicInteger remaining;
        private final List<T> results;
        private final List<Throwable> errors;

        PromiseAggregator(List<Promise<? extends T>> promises) {
            this.promises = promises;
            this.deferred = Promises.defer();
            this.remaining = new AtomicInteger(promises.size());
            this.results = createResultsList(promises.size());
            this.errors = new ArrayList<>();
        }

        Promise<List<T>> execute() {
            for (int i = 0; i < promises.size(); i++) {
                int index = i;
                Promise<? extends T> promise = promises.get(i);

                promise.onSuccess(result -> {
                    synchronized (this) {
                        results.set(index, result);
                    }
                    checkCompletion();
                });

                promise.onError(error -> {
                    synchronized (this) {
                        errors.add(error);
                    }
                    checkCompletion();
                });

                promise.onCancelled(() -> {
                    synchronized (this) {
                        errors.add(new io.axiom.concurrent.promise.error.CancellationException());
                    }
                    checkCompletion();
                });
            }
            return deferred.promise();
        }

        private void checkCompletion() {
            if (remaining.decrementAndGet() == 0) {
                synchronized (this) {
                    if (errors.isEmpty()) {
                        deferred.resolve(results);
                    } else {
                        deferred.reject(new AggregateException(errors));
                    }
                }
            }
        }

        private static <T> List<T> createResultsList(int size) {
            List<T> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(null);
            return list;
        }
    }
}
