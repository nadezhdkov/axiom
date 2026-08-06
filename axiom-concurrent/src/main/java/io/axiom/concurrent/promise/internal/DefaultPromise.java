package io.axiom.concurrent.promise.internal;

import io.axiom.concurrent.promise.Promise;
import io.axiom.concurrent.promise.PromiseState;
import io.axiom.concurrent.promise.Promises;
import io.axiom.concurrent.promise.RetryPolicy;
import io.axiom.concurrent.promise.error.TimeoutException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DefaultPromise<T> implements Promise<T> {

    private final CompletableFuture<T> future;

    /**
     * Non-null only for promises created via {@link #async(Supplier, Executor)}: re-invokes the
     * original supplier to produce a fresh {@link CompletableFuture} for each retry attempt.
     * Without this, {@link #retry(RetryPolicy)} would recover against the same already-failed
     * future on every attempt and never actually re-run the underlying operation — the same class
     * of bug fixed in {@code axiom-core}'s {@code Try.retry}.
     */
    private final Supplier<CompletableFuture<T>> regenerate;

    private DefaultPromise(CompletableFuture<T> future) {
        this(future, null);
    }

    private DefaultPromise(CompletableFuture<T> future, Supplier<CompletableFuture<T>> regenerate) {
        this.future = future;
        this.regenerate = regenerate;
    }

    // ==================== Factory Methods ====================

    public static <T> DefaultPromise<T> resolved(T value) {
        return new DefaultPromise<>(CompletableFuture.completedFuture(value));
    }

    public static <T> DefaultPromise<T> rejected(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return new DefaultPromise<>(future);
    }

    public static <T> DefaultPromise<T> cancelled() {
        return cancelled("Cancelled");
    }

    public static <T> DefaultPromise<T> cancelled(String reason) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new io.axiom.concurrent.promise.error.CancellationException(reason));
        return new DefaultPromise<>(future);
    }

    public static <T> DefaultPromise<T> async(Supplier<T> supplier) {
        return async(supplier, PromiseScheduler.getInstance().defaultExecutor());
    }

    public static <T> DefaultPromise<T> async(Supplier<T> supplier, Executor executor) {
        Supplier<CompletableFuture<T>> regenerate = () -> CompletableFuture.supplyAsync(supplier, executor);
        return new DefaultPromise<>(regenerate.get(), regenerate);
    }

    public static <T> DefaultPromise<T> fromCompletableFuture(CompletableFuture<T> future) {
        return new DefaultPromise<>(future);
    }

    public static DefaultPromise<Void> sleep(Duration duration) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        PromiseScheduler.getInstance().schedule(() -> future.complete(null), duration);
        return new DefaultPromise<>(future);
    }

    // ==================== Transformations ====================

    @Override
    public <U> Promise<U> map(Function<? super T, ? extends U> mapper) {
        return new DefaultPromise<>(future.thenApply(mapper));
    }

    @Override
    public <U> Promise<U> flatMap(Function<? super T, ? extends Promise<U>> mapper) {
        CompletableFuture<U> flattened = future.thenCompose(value -> mapper.apply(value).toCompletableFuture());
        return new DefaultPromise<>(flattened);
    }

    @Override
    public Promise<T> tap(Consumer<? super T> consumer) {
        CompletableFuture<T> tapped = future.whenComplete((value, error) -> {
            if (error == null) consumer.accept(value);
        });
        return new DefaultPromise<>(tapped);
    }

    @Override
    public Promise<T> filter(Predicate<? super T> predicate) {
        return filter(predicate, () -> new IllegalStateException("Filter predicate failed"));
    }

    @Override
    public Promise<T> filter(Predicate<? super T> predicate, Supplier<Throwable> errorSupplier) {
        return map(value -> {
            if (predicate.test(value)) return value;
            throw new RuntimeException(errorSupplier.get());
        });
    }

    // ==================== Error Handling ====================

    @Override
    public Promise<T> recover(Function<Throwable, ? extends T> recoveryFunction) {
        return new DefaultPromise<>(future.exceptionally(recoveryFunction));
    }

    @Override
    public Promise<T> recoverWith(Function<Throwable, ? extends Promise<T>> recoveryFunction) {
        CompletableFuture<T> recovered = future.handle((value, error) -> error != null
                        ? recoveryFunction.apply(unwrapException(error)).toCompletableFuture()
                        : CompletableFuture.completedFuture(value))
                .thenCompose(Function.identity());
        return new DefaultPromise<>(recovered);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Promise<T> catchError(Class<E> errorType, Function<E, ? extends T> handler) {
        return recover(error -> {
            if (errorType.isInstance(error)) return handler.apply((E) error);
            throw new RuntimeException(error);
        });
    }

    @Override
    public Promise<T> catchError(Function<Throwable, ? extends T> handler) {
        return recover(handler);
    }

    @Override
    public Promise<T> mapError(Function<Throwable, Throwable> errorMapper) {
        CompletableFuture<T> mapped = future.handle((value, error) -> {
                    if (error != null) throw new RuntimeException(errorMapper.apply(unwrapException(error)));
                    return value;
                })
                .thenCompose(CompletableFuture::completedFuture);
        return new DefaultPromise<>(mapped);
    }

    @Override
    public Promise<T> finallyDo(Runnable action) {
        return new DefaultPromise<>(future.whenComplete((value, error) -> action.run()));
    }

    // ==================== Timing & Control ====================

    @Override
    public Promise<T> timeout(Duration duration) {
        CompletableFuture<T> withTimeout = future.orTimeout(duration.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    if (error instanceof java.util.concurrent.TimeoutException) {
                        throw new RuntimeException(new TimeoutException(duration));
                    }
                    throw new RuntimeException(error);
                });
        return new DefaultPromise<>(withTimeout);
    }

    @Override
    public Promise<T> delay(Duration duration) {
        return flatMap(value -> DefaultPromise.sleep(duration).map(v -> value));
    }

    @Override
    public Promise<T> retry(RetryPolicy policy) {
        return new RetryHandler<>(this, policy).execute();
    }

    // ==================== State & Cancellation ====================

    @Override
    public PromiseState state() {
        if (!future.isDone()) return PromiseState.PENDING;
        if (future.isCancelled()) return PromiseState.CANCELLED;
        try {
            future.getNow(null);
            return PromiseState.FULFILLED;
        } catch (CompletionException e) {
            // cancel(String) completes exceptionally with our CancellationException rather than
            // calling Future#cancel — without this check that path would misreport REJECTED.
            if (unwrapException(e) instanceof io.axiom.concurrent.promise.error.CancellationException) {
                return PromiseState.CANCELLED;
            }
            return PromiseState.REJECTED;
        }
    }

    @Override
    public boolean cancel() {
        return future.cancel(true);
    }

    @Override
    public boolean cancel(String reason) {
        return future.completeExceptionally(new io.axiom.concurrent.promise.error.CancellationException(reason));
    }

    // ==================== Callbacks ====================

    @Override
    public Promise<T> onSuccess(Consumer<? super T> onSuccess) {
        future.thenAccept(onSuccess);
        return this;
    }

    @Override
    public Promise<T> onError(Consumer<Throwable> onError) {
        future.exceptionally(error -> {
            onError.accept(unwrapException(error));
            return null;
        });
        return this;
    }

    @Override
    public Promise<T> onCancelled(Runnable onCancelled) {
        future.exceptionally(error -> {
            if (error instanceof java.util.concurrent.CancellationException
                    || error instanceof io.axiom.concurrent.promise.error.CancellationException) {
                onCancelled.run();
            }
            return null;
        });
        return this;
    }

    @Override
    public Promise<T> onComplete(Runnable onComplete) {
        future.whenComplete((value, error) -> onComplete.run());
        return this;
    }

    // ==================== Blocking Operations ====================

    @Override
    public T get() throws Throwable {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw unwrapException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public T get(Duration timeout) throws Throwable {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw unwrapException(e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public T getOrDefault(T defaultValue) {
        try {
            return future.getNow(defaultValue);
        } catch (CompletionException e) {
            return defaultValue;
        }
    }

    @Override
    public T getOrElse(Supplier<? extends T> defaultSupplier) {
        try {
            return future.getNow(null);
        } catch (CompletionException e) {
            return defaultSupplier.get();
        }
    }

    // ==================== Interop ====================

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

    // ==================== Helpers ====================

    private static Throwable unwrapException(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) return error.getCause();
        if (error instanceof ExecutionException && error.getCause() != null) return error.getCause();
        return error;
    }

    private static final class RetryHandler<T> {
        private final DefaultPromise<T> initial;
        private final RetryPolicy policy;

        RetryHandler(DefaultPromise<T> initial, RetryPolicy policy) {
            this.initial = initial;
            this.policy = policy;
        }

        Promise<T> execute() {
            return attemptWithRetry(1, initial);
        }

        private Promise<T> attemptWithRetry(int attemptNumber, DefaultPromise<T> current) {
            return current.recoverWith(error -> {
                if (attemptNumber > policy.maxAttempts() || !policy.shouldRetry(error)) {
                    return Promises.error(error);
                }

                Duration backoff = policy.backoff(attemptNumber, error);
                // Re-invoke the original supplier for a genuinely new attempt when possible;
                // otherwise there is no underlying operation to redo (see `regenerate` javadoc).
                DefaultPromise<T> next = current.regenerate != null
                        ? new DefaultPromise<>(current.regenerate.get(), current.regenerate)
                        : current;

                if (backoff.isZero()) {
                    return attemptWithRetry(attemptNumber + 1, next);
                }
                return DefaultPromise.sleep(backoff).flatMap(v -> attemptWithRetry(attemptNumber + 1, next));
            });
        }
    }
}
