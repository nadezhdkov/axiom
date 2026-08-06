package io.axiom.concurrent.box;

/**
 * Non-thread-safe {@link Box} implementation with plain field semantics: no synchronization,
 * no volatile semantics, no atomic guarantees. Intended for single-threaded or thread-confined
 * usage where synchronization would be pure overhead.
 *
 * @param <T> the type of the boxed value
 */
public final class PlainBox<T> implements Box<T> {

    private T value;

    public PlainBox(T initial) {
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
