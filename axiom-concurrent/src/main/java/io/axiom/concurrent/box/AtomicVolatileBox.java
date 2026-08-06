package io.axiom.concurrent.box;

/**
 * {@link Box} implementation with {@code volatile} visibility semantics: writes are immediately
 * visible to other threads, but no atomic read-modify-write operations are provided (no CAS).
 * Sits between {@link PlainBox} and {@link AtomicBox} in terms of concurrency guarantees — prefer
 * this when the value is replaced wholesale and updates never depend on the previous value.
 *
 * @param <T> the type of the boxed value
 */
public final class AtomicVolatileBox<T> implements Box<T> {

    private volatile T value;

    public AtomicVolatileBox(T initial) {
        this.value = initial;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }
}
