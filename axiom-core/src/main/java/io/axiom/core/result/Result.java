package io.axiom.core.result;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Algebraic data type representing a computation result: either {@code Ok(value)} or {@code Err(error)}.
 *
 * <h2>Overview</h2>
 * {@code Result} is a lightweight alternative to exceptions and {@link Optional} for APIs that need to
 * return either a value or a well-defined, expected domain error type.
 *
 * <h2>Try vs Result vs Maybe</h2>
 * See {@code io.axiom.core} package documentation for the boundary between the three outcome types.
 * In short: use {@code Result<T,E>} when the failure is an expected domain outcome the caller must
 * handle explicitly, as opposed to {@link Try}, which captures unexpected {@link Throwable}s from
 * Java APIs.
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * Result<Integer, String> parsed = parseInt("123").map(n -> n * 2);
 *
 * String msg = parsed.fold(
 *     ok  -> "Value = " + ok,
 *     err -> "Error: " + err
 * );
 * }</pre>
 *
 * @param <T> success value type
 * @param <E> error type
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    record Ok<T, E>(T value) implements Result<T, E> { }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err {
            Objects.requireNonNull(error, "error");
        }
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    /**
     * Executes a supplier and captures any thrown exception as {@code Err} using {@code errorMapper}.
     * Bridges an exception-throwing API into a {@code Result}-based workflow.
     */
    static <T, E> Result<T, E> from(
            CheckedSupplier<? extends T> supplier,
            Function<? super Throwable, ? extends E> errorMapper
    ) {
        Objects.requireNonNull(supplier, "supplier");
        Objects.requireNonNull(errorMapper, "errorMapper");
        try {
            return ok(supplier.get());
        } catch (Throwable t) {
            return err(errorMapper.apply(t));
        }
    }

    default boolean isOk() {
        return this instanceof Ok<?, ?>;
    }

    default boolean isErr() {
        return this instanceof Err<?, ?>;
    }

    default T orNull() {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> null;
        };
    }

    default Optional<T> toOptional() {
        return Optional.ofNullable(orNull());
    }

    default T orElse(T fallback) {
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> fallback;
        };
    }

    default T orElseGet(Supplier<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> fallback.get();
        };
    }

    default T orElseThrow() {
        return orElseThrow(e -> new IllegalStateException(String.valueOf(e)));
    }

    default T orElseThrow(Function<? super E, ? extends RuntimeException> exMapper) {
        Objects.requireNonNull(exMapper, "exMapper");
        return switch (this) {
            case Ok<T, E> ok -> ok.value();
            case Err<T, E> err -> throw exMapper.apply(err.error());
        };
    }

    default E errorOrThrow() {
        return switch (this) {
            case Ok<T, E> ok -> throw new IllegalStateException("Result is Ok");
            case Err<T, E> err -> err.error();
        };
    }

    default <U> Result<U, E> map(Function<? super T, ? extends U> fn) {
        Objects.requireNonNull(fn, "fn");
        return switch (this) {
            case Ok<T, E> ok -> Result.ok(fn.apply(ok.value()));
            case Err<T, E> err -> Result.err(err.error());
        };
    }

    default <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> fn) {
        Objects.requireNonNull(fn, "fn");
        return switch (this) {
            case Ok<T, E> ok -> Objects.requireNonNull(fn.apply(ok.value()), "flatMap returned null");
            case Err<T, E> err -> Result.err(err.error());
        };
    }

    default <F> Result<T, F> mapError(Function<? super E, ? extends F> fn) {
        Objects.requireNonNull(fn, "fn");
        return switch (this) {
            case Ok<T, E> ok -> Result.ok(ok.value());
            case Err<T, E> err -> Result.err(fn.apply(err.error()));
        };
    }

    default Result<T, E> tap(Consumer<? super T> c) {
        Objects.requireNonNull(c, "c");
        if (this instanceof Ok<T, E>(T value)) c.accept(value);
        return this;
    }

    default Result<T, E> tapError(Consumer<? super E> c) {
        Objects.requireNonNull(c, "c");
        if (this instanceof Err<T, E>(E error)) c.accept(error);
        return this;
    }

    default Result<T, E> recover(Function<? super E, ? extends T> fn) {
        Objects.requireNonNull(fn, "fn");
        return switch (this) {
            case Ok<T, E> ok -> this;
            case Err<T, E> err -> Result.ok(fn.apply(err.error()));
        };
    }

    default Result<T, E> recoverWith(Function<? super E, ? extends Result<T, E>> fn) {
        Objects.requireNonNull(fn, "fn");
        return switch (this) {
            case Ok<T, E> ok -> this;
            case Err<T, E> err -> Objects.requireNonNull(fn.apply(err.error()), "recoverWith returned null");
        };
    }

    default <R> R fold(Function<? super T, ? extends R> onOk, Function<? super E, ? extends R> onErr) {
        Objects.requireNonNull(onOk, "onOk");
        Objects.requireNonNull(onErr, "onErr");
        return switch (this) {
            case Ok<T, E> ok -> onOk.apply(ok.value());
            case Err<T, E> err -> onErr.apply(err.error());
        };
    }

    /** Supplier that may throw any {@link Throwable}, used with {@link #from}. */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Throwable;
    }
}
