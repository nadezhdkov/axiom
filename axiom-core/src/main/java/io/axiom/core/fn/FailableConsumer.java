package io.axiom.core.fn;

/**
 * A {@link java.util.function.Consumer}-like operation that is permitted to throw a checked exception.
 *
 * @param <T> the type of value consumed
 */
@FunctionalInterface
public interface FailableConsumer<T> {
    void accept(T value) throws Exception;
}
