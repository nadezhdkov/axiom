package io.axiom.concurrent.promise;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An asynchronous computation that will eventually produce a value, fail with an error, or be
 * cancelled. Immutable and thread-safe: each transformation returns a new {@code Promise}.
 *
 * @param <T> the type of the computed value
 */
public interface Promise<T> {

    // ==================== Transformations ====================

    <U> Promise<U> map(Function<? super T, ? extends U> mapper);

    /** Chains another asynchronous operation that depends on this Promise's result. */
    <U> Promise<U> flatMap(Function<? super T, ? extends Promise<U>> mapper);

    /** Alias for {@link #flatMap}. */
    default <U> Promise<U> then(Function<? super T, ? extends Promise<U>> mapper) {
        return flatMap(mapper);
    }

    default Promise<T> thenDo(Function<? super T, ? extends Promise<T>> action) {
        return flatMap(action);
    }

    /** Executes a side effect on success, without changing the value. */
    Promise<T> tap(Consumer<? super T> consumer);

    /** Rejects the Promise if the predicate returns false. */
    Promise<T> filter(Predicate<? super T> predicate);

    Promise<T> filter(Predicate<? super T> predicate, Supplier<Throwable> errorSupplier);

    // ==================== Error Handling ====================

    Promise<T> recover(Function<Throwable, ? extends T> recoveryFunction);

    Promise<T> recoverWith(Function<Throwable, ? extends Promise<T>> recoveryFunction);

    <E extends Throwable> Promise<T> catchError(Class<E> errorType, Function<E, ? extends T> handler);

    Promise<T> catchError(Function<Throwable, ? extends T> handler);

    /** Transforms the error without recovering (still rejected, just with a mapped error). */
    Promise<T> mapError(Function<Throwable, Throwable> errorMapper);

    /** Executes an action regardless of success/failure/cancellation, like try-finally. */
    Promise<T> finallyDo(Runnable action);

    // ==================== Timing & Control ====================

    /** Rejects with {@link io.axiom.concurrent.promise.error.TimeoutException} if not completed in time. */
    Promise<T> timeout(Duration duration);

    Promise<T> delay(Duration duration);

    Promise<T> retry(RetryPolicy policy);

    /** Retries a fixed number of times with no delay. */
    default Promise<T> retry(int maxAttempts) {
        return retry(RetryPolicy.configure().maxAttempts(maxAttempts).build());
    }

    // ==================== State & Cancellation ====================

    PromiseState state();

    default boolean isPending() {
        return state() == PromiseState.PENDING;
    }

    default boolean isFulfilled() {
        return state() == PromiseState.FULFILLED;
    }

    default boolean isRejected() {
        return state() == PromiseState.REJECTED;
    }

    default boolean isCancelled() {
        return state() == PromiseState.CANCELLED;
    }

    boolean cancel();

    boolean cancel(String reason);

    // ==================== Callbacks ====================

    Promise<T> onSuccess(Consumer<? super T> onSuccess);

    Promise<T> onError(Consumer<Throwable> onError);

    Promise<T> onCancelled(Runnable onCancelled);

    Promise<T> onComplete(Runnable onComplete);

    // ==================== Blocking Operations ====================

    T get() throws Throwable;

    T get(Duration timeout) throws Throwable;

    T getOrDefault(T defaultValue);

    T getOrElse(Supplier<? extends T> defaultSupplier);

    // ==================== Interop ====================

    CompletableFuture<T> toCompletableFuture();

    default Future<T> toFuture() {
        return toCompletableFuture();
    }
}
