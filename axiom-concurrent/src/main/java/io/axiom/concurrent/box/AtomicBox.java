package io.axiom.concurrent.box;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe {@link Box} implementation backed by {@link AtomicReference}, providing lock-free
 * access and CAS-based updates ({@link #compareAndSet(Object, Object)}). Preferred for highly
 * concurrent scenarios where multiple threads update the value.
 *
 * @param <T> the type of the boxed value
 */
public final class AtomicBox<T> implements Box<T> {

    private final AtomicReference<T> reference;

    public AtomicBox(T initial) {
        this.reference = new AtomicReference<>(initial);
    }

    @Override
    public T get() {
        return reference.get();
    }

    @Override
    public void set(T value) {
        reference.set(value);
    }

    @Override
    public T getAndSet(T value) {
        return reference.getAndSet(value);
    }

    @Override
    public boolean compareAndSet(T expect, T update) {
        return reference.compareAndSet(expect, update);
    }
}
