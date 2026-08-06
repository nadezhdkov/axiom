package io.axiom.concurrent.promise;

import io.axiom.concurrent.promise.combinators.PromiseAll;
import io.axiom.concurrent.promise.combinators.PromiseAny;
import io.axiom.concurrent.promise.combinators.PromiseRace;
import io.axiom.concurrent.promise.internal.DefaultDeferred;
import io.axiom.concurrent.promise.internal.DefaultPromise;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Main entry point for creating and combining {@link Promise}s.
 *
 * <pre>{@code
 * Promise<String> p1 = Promises.value("hello");
 * Promise<Data> p2 = Promises.async(() -> loadData());
 * Promise<List<String>> all = Promises.all(p1, p2.map(Object::toString));
 * }</pre>
 */
public final class Promises {

    private Promises() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== Simple Creation ====================

    public static <T> Promise<T> value(T value) {
        return DefaultPromise.resolved(value);
    }

    public static <T> Promise<T> error(Throwable error) {
        return DefaultPromise.rejected(error);
    }

    public static <T> Promise<T> cancelled() {
        return DefaultPromise.cancelled();
    }

    public static <T> Promise<T> cancelled(String reason) {
        return DefaultPromise.cancelled(reason);
    }

    // ==================== Async Creation ====================

    /** Executes the supplier asynchronously on {@link io.axiom.concurrent.Tasks#executor()} by default. */
    public static <T> Promise<T> async(Supplier<T> supplier) {
        return DefaultPromise.async(supplier);
    }

    public static <T> Promise<T> async(Supplier<T> supplier, Executor executor) {
        return DefaultPromise.async(supplier, executor);
    }

    public static <T> Promise<T> call(Callable<T> callable) {
        return async(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Resolves to {@code null} when the runnable completes. */
    public static Promise<Void> run(Runnable runnable) {
        return async(() -> {
            runnable.run();
            return null;
        });
    }

    public static Promise<Void> run(Runnable runnable, Executor executor) {
        return async(() -> {
            runnable.run();
            return null;
        }, executor);
    }

    // ==================== Deferred ====================

    public static <T> Deferred<T> defer() {
        return new DefaultDeferred<>();
    }

    // ==================== CompletableFuture Bridge ====================

    public static <T> Promise<T> from(CompletableFuture<T> future) {
        return DefaultPromise.fromCompletableFuture(future);
    }

    /** Polls the given Future on the default executor. */
    public static <T> Promise<T> from(java.util.concurrent.Future<T> future) {
        return async(() -> {
            try {
                return future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ==================== Combinators ====================

    /** Resolves when all promises succeed (fail-fast on first error). */
    @SafeVarargs
    public static <T> Promise<List<T>> all(Promise<? extends T>... promises) {
        return PromiseAll.of(Arrays.asList(promises));
    }

    public static <T> Promise<List<T>> all(List<Promise<? extends T>> promises) {
        return PromiseAll.of(promises);
    }

    /** Resolves with the first success; rejects with an AggregateException only if all fail. */
    @SafeVarargs
    public static <T> Promise<T> any(Promise<? extends T>... promises) {
        return PromiseAny.of(Arrays.asList(promises));
    }

    public static <T> Promise<T> any(List<Promise<? extends T>> promises) {
        return PromiseAny.of(promises);
    }

    /** Completes with the first promise to complete, success or failure. */
    @SafeVarargs
    public static <T> Promise<T> race(Promise<? extends T>... promises) {
        return PromiseRace.of(Arrays.asList(promises));
    }

    public static <T> Promise<T> race(List<Promise<? extends T>> promises) {
        return PromiseRace.of(promises);
    }

    // ==================== Timing ====================

    public static Promise<Void> delay(Duration duration) {
        return DefaultPromise.sleep(duration);
    }

    public static <T> Promise<T> delay(T value, Duration duration) {
        return delay(duration).map(v -> value);
    }

    // ==================== Utilities ====================

    /** Wraps a potentially throwing operation; a thrown exception rejects the returned Promise. */
    public static <T> Promise<T> wrap(Supplier<T> supplier) {
        try {
            return value(supplier.get());
        } catch (Throwable t) {
            return error(t);
        }
    }

    /** A promise that never completes — useful for testing timeouts. */
    @SuppressWarnings("unchecked")
    public static <T> Promise<T> never() {
        return (Promise<T>) defer().promise();
    }
}
