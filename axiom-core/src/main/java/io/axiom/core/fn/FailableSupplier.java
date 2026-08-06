package io.axiom.core.fn;

/**
 * A {@link java.util.function.Supplier}-like operation that is permitted to throw a checked exception.
 *
 * @param <T> the type of value supplied
 */
@FunctionalInterface
public interface FailableSupplier<T> {
    T get() throws Exception;
}
