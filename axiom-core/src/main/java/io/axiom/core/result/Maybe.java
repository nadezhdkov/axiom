package io.axiom.core.result;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents an optional value that may either be present ({@code Some}) or absent
 * ({@code None}), optionally carrying a human-readable reason for the absence.
 *
 * <h2>Overview</h2>
 * {@code Maybe} is a functional container similar to {@link Optional}, intended for absence
 * without an associated cause. See {@code io.axiom.core} package documentation for the boundary
 * against {@link Result} and {@link Try}.
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * Maybe<String> nickname = user.nickname(); // Some | None
 * String display = nickname.orElse(user.fullName());
 * }</pre>
 *
 * @param <T> the type of the contained value
 */
public sealed interface Maybe<T> permits Maybe.Some, Maybe.None {

    boolean isPresent();

    default boolean isEmpty() {
        return !isPresent();
    }

    T get();

    /**
     * Optional human-readable reason describing why the value is absent. Meaningful only
     * when this instance represents {@link None}.
     */
    default String reason() {
        return null;
    }

    static <T> Maybe<T> of(T value) {
        return value == null ? none() : new Some<>(value);
    }

    static <T> Maybe<T> some(T value) {
        return new Some<>(Objects.requireNonNull(value, "value"));
    }

    static <T> Maybe<T> none() {
        return None.instance();
    }

    static <T> Maybe<T> none(String reason) {
        return new None<>(Objects.requireNonNull(reason, "reason"));
    }

    @SuppressWarnings("unchecked")
    static <T> Maybe<T> from(Optional<? extends T> optional) {
        Objects.requireNonNull(optional, "optional");
        return (Maybe<T>) optional.map(Maybe::some).orElseGet(Maybe::none);
    }

    default <U> Maybe<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isEmpty()) return castNone();
        return Maybe.of(mapper.apply(get()));
    }

    default <U> Maybe<U> flatMap(Function<? super T, ? extends Maybe<U>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (isEmpty()) return castNone();
        return Objects.requireNonNull(mapper.apply(get()), "flatMap returned null");
    }

    default Maybe<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (isEmpty()) return this;
        return predicate.test(get()) ? this : none("Predicate does not hold");
    }

    default Maybe<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action");
        if (isPresent()) action.accept(get());
        return this;
    }

    default Maybe<T> onSome(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action");
        if (isPresent()) action.accept(get());
        return this;
    }

    default Maybe<T> onNone(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (isEmpty()) action.run();
        return this;
    }

    default T orNull() {
        return isPresent() ? get() : null;
    }

    default T orElse(T fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return isPresent() ? get() : fallback;
    }

    default T orElseNullable(T fallback) {
        return isPresent() ? get() : fallback;
    }

    default T orElseGet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        if (isPresent()) return get();
        return Objects.requireNonNull(supplier.get(), "orElseGet supplier returned null");
    }

    default <X extends RuntimeException> T orElseThrow(Supplier<X> exSupplier) {
        Objects.requireNonNull(exSupplier, "exSupplier");
        if (isPresent()) return get();
        throw exSupplier.get();
    }

    default T orElseThrow() {
        if (isPresent()) return get();
        String r = reason();
        throw new NoSuchElementException(r == null ? "Maybe is empty" : "Maybe is empty: " + r);
    }

    default Optional<T> toOptional() {
        return isPresent() ? Optional.ofNullable(get()) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private <U> Maybe<U> castNone() {
        return (Maybe<U>) this;
    }

    record Some<T>(T value) implements Maybe<T> {
        public Some {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public String toString() {
            return "Some[" + value + "]";
        }
    }

    record None<T>(String reason) implements Maybe<T> {

        private static final None<?> EMPTY = new None<>(null);

        @SuppressWarnings("unchecked")
        static <T> None<T> instance() {
            return (None<T>) EMPTY;
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public T get() {
            throw new NoSuchElementException(reason == null ? "Maybe is empty" : "Maybe is empty: " + reason);
        }

        @Override
        public String toString() {
            return reason == null ? "None" : "None[" + reason + "]";
        }
    }
}
