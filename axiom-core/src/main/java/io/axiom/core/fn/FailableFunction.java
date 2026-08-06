package io.axiom.core.fn;

/**
 * A {@link java.util.function.Function}-like operation that is permitted to throw a checked exception.
 *
 * @param <T> the input type
 * @param <R> the result type
 */
@FunctionalInterface
public interface FailableFunction<T, R> {
    R apply(T value) throws Exception;
}
