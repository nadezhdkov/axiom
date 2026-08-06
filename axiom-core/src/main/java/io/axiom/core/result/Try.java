package io.axiom.core.result;

import io.axiom.core.fn.FailableConsumer;
import io.axiom.core.fn.FailableFunction;
import io.axiom.core.fn.FailableRunnable;
import io.axiom.core.fn.FailableSupplier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A container representing the result of an operation that may succeed with a value or fail
 * with a {@link Throwable}.
 *
 * <h2>Overview</h2>
 * {@code Try} is a functional error-handling container designed to replace explicit
 * {@code try/catch} blocks in fluent pipelines, capturing exceptions thrown by Java APIs. See
 * {@code io.axiom.core} package documentation for the boundary against {@link Result} and
 * {@link Maybe}.
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"))
 *         .map(n -> n * 2)
 *         .recover(NumberFormatException.class, ex -> 0);
 * }</pre>
 *
 * <h2>Design notes</h2>
 * {@code Try} is immutable; every transformation returns a new instance. Retrying an operation
 * always re-invokes the original supplier — see {@link #retry(int, Duration, Supplier)} and
 * {@link #retryWithBackoff(int, Duration, Supplier)}, the only supported retry entry points.
 *
 * @param <T> the type of the successfully computed value
 */
public final class Try<T> {

    private final T value;
    private final Throwable failure;

    private Try(T value, Throwable failure) {
        this.value = value;
        this.failure = failure;
    }

    // ==================== FACTORY METHODS ====================

    public static <T> Try<T> success(T value) {
        return new Try<>(value, null);
    }

    public static <T> Try<T> failure(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        return new Try<>(null, failure);
    }

    /** Executes the supplier and captures any thrown exception into a failure. */
    public static <T> Try<T> of(FailableSupplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return success(supplier.get());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /** Executes the runnable and captures any thrown exception into a failure. */
    public static Try<Void> run(FailableRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        try {
            runnable.run();
            return success(null);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public static <T> Try<T> fromOptional(Optional<T> optional) {
        return fromOptional(optional, () -> new NoSuchElementException("Optional was empty"));
    }

    public static <T> Try<T> fromOptional(Optional<T> optional, Supplier<? extends Throwable> exceptionSupplier) {
        return optional.map(Try::success).orElseGet(() -> failure(exceptionSupplier.get()));
    }

    public static <T> Try<T> fromNullable(T value) {
        return value != null ? success(value) : failure(new NullPointerException("Value was null"));
    }

    public static <T> Try<T> fromNullable(T value, Supplier<? extends Throwable> exceptionSupplier) {
        return value != null ? success(value) : failure(exceptionSupplier.get());
    }

    // ==================== STATE CHECKING ====================

    public boolean isSuccess() {
        return failure == null;
    }

    public boolean isFailure() {
        return failure != null;
    }

    public boolean isFailureOf(Class<? extends Throwable> exceptionClass) {
        return isFailure() && exceptionClass.isInstance(failure);
    }

    public boolean matches(Predicate<? super T> predicate) {
        return isSuccess() && predicate.test(value);
    }

    // ==================== VALUE EXTRACTION ====================

    /** Returns the value if successful; otherwise sneaky-throws the wrapped exception unchecked. */
    public T orThrow() {
        if (isFailure()) {
            throw sneakyThrow(failure);
        }
        return value;
    }

    /** Returns the value if successful; otherwise re-throws the original checked exception. */
    public T checkedGet() throws Exception {
        if (isSuccess()) return value;
        if (failure instanceof Exception e) throw e;
        if (failure instanceof Error e) throw e;
        throw new RuntimeException(failure);
    }

    public T getOrElse(T fallback) {
        return isSuccess() ? value : fallback;
    }

    public T orElse(T fallback) {
        return getOrElse(fallback);
    }

    public T getOrElseGet(Supplier<? extends T> fallback) {
        return isSuccess() ? value : fallback.get();
    }

    public T orElseGet(Supplier<? extends T> fallback) {
        return getOrElseGet(fallback);
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isFailure()) throw exceptionSupplier.get();
        return value;
    }

    public <X extends Throwable> T orElseThrow(Function<Throwable, X> exceptionMapper) throws X {
        if (isFailure()) throw exceptionMapper.apply(failure);
        return value;
    }

    public T getOrNull() {
        return value;
    }

    public Throwable getFailureOrNull() {
        return failure;
    }

    @SuppressWarnings("unchecked")
    public <E extends Throwable> E getFailureAs(Class<E> exceptionClass) {
        return isFailureOf(exceptionClass) ? (E) failure : null;
    }

    public Optional<Throwable> exception() {
        return Optional.ofNullable(failure);
    }

    // ==================== TRANSFORMATIONS ====================

    public <R> Try<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) return failure(failure);
        return of(() -> mapper.apply(value));
    }

    /** Like {@link #map(Function)}, but the mapper is allowed to throw a checked exception. */
    public <R> Try<R> mapTry(FailableFunction<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) return failure(failure);
        return of(() -> mapper.apply(value));
    }

    public <R> Try<R> flatMap(Function<? super T, Try<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isFailure()) return failure(failure);
        try {
            return mapper.apply(value);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public Try<T> mapFailure(Function<Throwable, Throwable> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isSuccess()) return this;
        try {
            return failure(mapper.apply(failure));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public <R> R fold(Function<Throwable, R> failureMapper, Function<? super T, R> successMapper) {
        return isSuccess() ? successMapper.apply(value) : failureMapper.apply(failure);
    }

    public <R> R transform(Function<? super T, R> successMapper, Function<Throwable, R> failureMapper) {
        return fold(failureMapper, successMapper);
    }

    public Try<Throwable> swap() {
        return isSuccess()
                ? failure(new IllegalStateException("Swap called on success: " + value))
                : success(failure);
    }

    // ==================== FILTERING ====================

    public Try<T> filter(Predicate<? super T> predicate) {
        return filter(predicate, "Filter predicate failed");
    }

    public Try<T> filter(Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate");
        if (isFailure()) return this;
        try {
            return predicate.test(value) ? this : failure(new NoSuchElementException(message));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public Try<T> filter(Predicate<? super T> predicate, Supplier<? extends Throwable> exceptionSupplier) {
        Objects.requireNonNull(predicate, "predicate");
        if (isFailure()) return this;
        try {
            return predicate.test(value) ? this : failure(exceptionSupplier.get());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public Try<T> filterNot(Predicate<? super T> predicate) {
        return filter(predicate.negate());
    }

    public Try<T> filterNotNull() {
        return filter(Objects::nonNull, "Value was null");
    }

    // ==================== RECOVERY ====================

    public Try<T> recover(Function<Throwable, ? extends T> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        if (isSuccess()) return this;
        return of(() -> recovery.apply(failure));
    }

    public Try<T> recover(T fallback) {
        return isSuccess() ? this : success(fallback);
    }

    public Try<T> recoverWith(Function<Throwable, Try<T>> recovery) {
        Objects.requireNonNull(recovery, "recovery");
        if (isSuccess()) return this;
        try {
            return recovery.apply(failure);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /** Type-based recovery: only recovers if the failure is an instance of {@code exceptionClass}. */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Try<T> recover(Class<E> exceptionClass, Function<? super E, ? extends T> recovery) {
        Objects.requireNonNull(exceptionClass, "exceptionClass");
        Objects.requireNonNull(recovery, "recovery");
        if (isSuccess() || !exceptionClass.isInstance(failure)) return this;
        return of(() -> recovery.apply((E) failure));
    }

    @SuppressWarnings("unchecked")
    public <E extends Throwable> Try<T> recoverWith(Class<E> exceptionClass, Function<? super E, Try<T>> recovery) {
        Objects.requireNonNull(exceptionClass, "exceptionClass");
        Objects.requireNonNull(recovery, "recovery");
        if (isSuccess() || !exceptionClass.isInstance(failure)) return this;
        try {
            return recovery.apply((E) failure);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    // ==================== SIDE EFFECTS ====================

    public Try<T> peek(Consumer<? super T> action) {
        return onSuccess(action);
    }

    public Try<T> onSuccess(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action");
        if (isSuccess()) {
            try {
                action.accept(value);
            } catch (Throwable t) {
                return failure(t);
            }
        }
        return this;
    }

    public Try<T> onFailure(Consumer<Throwable> action) {
        Objects.requireNonNull(action, "action");
        if (isFailure()) {
            action.accept(failure);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <E extends Throwable> Try<T> onFailureOf(Class<E> exceptionClass, Consumer<E> action) {
        if (isFailureOf(exceptionClass)) {
            action.accept((E) failure);
        }
        return this;
    }

    public Try<T> onComplete(Runnable action) {
        Objects.requireNonNull(action, "action");
        action.run();
        return this;
    }

    public Try<T> andThen(FailableConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return flatMap(v -> of(() -> {
            consumer.accept(v);
            return v;
        }));
    }

    public Try<T> handle(Consumer<? super T> successAction, Consumer<Throwable> failureAction) {
        return isSuccess() ? onSuccess(successAction) : onFailure(failureAction);
    }

    // ==================== CONVERSION ====================

    public Optional<T> toOptional() {
        return Optional.ofNullable(value);
    }

    public Stream<T> stream() {
        return isSuccess() ? Stream.of(value) : Stream.empty();
    }

    public List<T> toList() {
        return isSuccess() ? Collections.singletonList(value) : Collections.emptyList();
    }

    public CompletableFuture<T> toCompletableFuture() {
        return isSuccess() ? CompletableFuture.completedFuture(value) : CompletableFuture.failedFuture(failure);
    }

    // ==================== STATIC UTILITIES ====================

    /**
     * Retries the given supplier with a fixed delay between attempts, re-invoking the original
     * supplier on every attempt. Fixes a known bug where a prior instance-level
     * {@code retry(int)} implementation did not re-invoke the original supplier.
     */
    public static <T> Try<T> retry(int maxAttempts, Duration delayBetweenAttempts, Supplier<T> supplier) {
        Objects.requireNonNull(delayBetweenAttempts, "delayBetweenAttempts");
        Objects.requireNonNull(supplier, "supplier");
        Try<T> result = of(supplier::get);

        for (int attempt = 1; attempt < maxAttempts && result.isFailure(); attempt++) {
            try {
                Thread.sleep(delayBetweenAttempts.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return failure(e);
            }
            result = of(supplier::get);
        }

        return result;
    }

    /** Retries with exponential backoff, doubling the delay after each failed attempt. */
    public static <T> Try<T> retryWithBackoff(int maxAttempts, Duration initialDelay, Supplier<T> supplier) {
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(supplier, "supplier");
        Try<T> result = of(supplier::get);
        long delayMillis = initialDelay.toMillis();

        for (int attempt = 1; attempt < maxAttempts && result.isFailure(); attempt++) {
            try {
                Thread.sleep(delayMillis);
                delayMillis *= 2;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return failure(e);
            }
            result = of(supplier::get);
        }

        return result;
    }

    public static <T1, T2, R> Try<R> combine(Try<T1> try1, Try<T2> try2, BiFunction<T1, T2, R> combiner) {
        if (try1.isFailure()) return failure(try1.failure);
        if (try2.isFailure()) return failure(try2.failure);
        return of(() -> combiner.apply(try1.value, try2.value));
    }

    public static <T1, T2, T3, R> Try<R> combine(
            Try<T1> try1, Try<T2> try2, Try<T3> try3, TriFunction<T1, T2, T3, R> combiner) {
        if (try1.isFailure()) return failure(try1.failure);
        if (try2.isFailure()) return failure(try2.failure);
        if (try3.isFailure()) return failure(try3.failure);
        return of(() -> combiner.apply(try1.value, try2.value, try3.value));
    }

    /** Sequences a collection of {@code Try} into a {@code Try} of a list; short-circuits on the first failure. */
    public static <T> Try<List<T>> sequence(Collection<Try<T>> tries) {
        List<T> results = new ArrayList<>(tries.size());
        for (Try<T> t : tries) {
            if (t.isFailure()) return failure(t.failure);
            results.add(t.value);
        }
        return success(results);
    }

    public static <T> Try<List<T>> sequence(Stream<Try<T>> tries) {
        return sequence(tries.toList());
    }

    public static <T, R> Try<List<R>> traverse(Collection<T> collection, Function<T, Try<R>> mapper) {
        return sequence(collection.stream().map(mapper).toList());
    }

    public static <T> List<T> collectSuccesses(Collection<Try<T>> tries) {
        return tries.stream().filter(Try::isSuccess).map(t -> t.value).toList();
    }

    public static List<Throwable> collectFailures(Collection<? extends Try<?>> tries) {
        return tries.stream().filter(Try::isFailure).map(Try::getFailureOrNull).toList();
    }

    public static <T> Try<T> withTimeout(Duration timeout, Supplier<T> supplier) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit(supplier::get);
            return success(future.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            return failure(new TimeoutException("Operation timed out after " + timeout));
        } catch (Exception e) {
            return failure(e);
        } finally {
            executor.shutdownNow();
        }
    }

    @SafeVarargs
    public static <T> Try<List<T>> parallel(Supplier<T>... suppliers) {
        List<CompletableFuture<T>> futures = Arrays.stream(suppliers)
                .map(CompletableFuture::supplyAsync)
                .toList();
        try {
            return success(futures.stream().map(CompletableFuture::join).toList());
        } catch (Throwable t) {
            return failure(t);
        }
    }

    // ==================== HELPER METHODS ====================

    private static RuntimeException sneakyThrow(Throwable t) {
        return Try.<RuntimeException>sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneakyThrow0(Throwable t) throws E {
        throw (E) t;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return isSuccess()
                ? "Try.Success[" + value + "]"
                : "Try.Failure[" + failure.getClass().getSimpleName() + ": " + failure.getMessage() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Try<?> other)) return false;
        return Objects.equals(value, other.value) && Objects.equals(failure, other.failure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, failure);
    }

    @FunctionalInterface
    public interface TriFunction<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }
}
