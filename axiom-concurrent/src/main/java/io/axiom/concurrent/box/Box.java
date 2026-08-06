package io.axiom.concurrent.box;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A minimal mutable container ("box") for holding a single value.
 *
 * <h2>Thread-safety and implementations</h2>
 * Thread-safety depends on which factory method you use:
 * <ul>
 *   <li>{@link #of(Object)}: CAS-capable, thread-safe ({@link AtomicBox})</li>
 *   <li>{@link #volatileBox(Object)}: visibility guarantees only, no CAS ({@link AtomicVolatileBox})</li>
 *   <li>{@link #plain(Object)}: no concurrency guarantees, fastest ({@link PlainBox})</li>
 * </ul>
 *
 * <p>{@link #getAndUpdate(UnaryOperator)} and {@link #updateAndGet(UnaryOperator)} rely on
 * {@link #compareAndSet(Object, Object)} and therefore require a CAS-capable implementation.
 *
 * @param <T> the value type stored in the box
 */
public interface Box<T> {

    T get();

    void set(T value);

    default T getAndSet(T value) {
        T prev = get();
        set(value);
        return prev;
    }

    default boolean isNull() {
        return get() == null;
    }

    default Optional<T> toOptional() {
        return Optional.ofNullable(get());
    }

    /**
     * @throws UnsupportedOperationException if CAS is not supported by this box implementation.
     */
    default boolean compareAndSet(T expect, T update) {
        throw new UnsupportedOperationException("compareAndSet not supported");
    }

    /** Requires CAS support; the update function may be invoked multiple times under contention. */
    default T getAndUpdate(UnaryOperator<T> updateFn) {
        Objects.requireNonNull(updateFn, "updateFn");
        while (true) {
            T prev = get();
            T next = updateFn.apply(prev);
            if (compareAndSet(prev, next)) return prev;
        }
    }

    /** Requires CAS support; the update function may be invoked multiple times under contention. */
    default T updateAndGet(UnaryOperator<T> updateFn) {
        Objects.requireNonNull(updateFn, "updateFn");
        while (true) {
            T prev = get();
            T next = updateFn.apply(prev);
            if (compareAndSet(prev, next)) return next;
        }
    }

    default void update(UnaryOperator<T> updateFn) {
        updateAndGet(updateFn);
    }

    default T getOrElse(T defaultValue) {
        T v = get();
        return v != null ? v : defaultValue;
    }

    default void ifPresent(Consumer<? super T> c) {
        T v = get();
        if (v != null) c.accept(v);
    }

    /** Read-only mapped view; reflects the current source value on each {@link Box#get()} call. */
    default <R> BoxView<T, R> view(Function<? super T, ? extends R> mapper) {
        return new BoxView<>(this, mapper);
    }

    /** CAS-capable (atomic) box, suitable for concurrent updates. */
    static <T> Box<T> of(T initial) {
        return new AtomicBox<>(initial);
    }

    /** Non-thread-safe box with no concurrency guarantees; best for single-thread usage. */
    static <T> Box<T> plain(T initial) {
        return new PlainBox<>(initial);
    }

    /** Volatile-based box: visibility guarantees across threads, no atomic compound updates. */
    static <T> Box<T> volatileBox(T initial) {
        return new AtomicVolatileBox<>(initial);
    }

    /** Read-only mapped view of a source {@link Box}; {@link #set(Object)} always throws. */
    final class BoxView<S, T> implements Box<T> {
        private final Box<S> source;
        private final Function<? super S, ? extends T> mapper;

        public BoxView(Box<S> source, Function<? super S, ? extends T> mapper) {
            this.source = Objects.requireNonNull(source, "source");
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public T get() {
            return mapper.apply(source.get());
        }

        @Override
        public void set(T value) {
            throw new UnsupportedOperationException("view is read-only");
        }
    }
}
