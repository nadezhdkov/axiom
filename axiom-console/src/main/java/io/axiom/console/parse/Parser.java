package io.axiom.console.parse;

/** Converts a raw input line into {@code T}, or throws {@link ParseFailureException}. */
@FunctionalInterface
public interface Parser<T> {
    T parse(String raw) throws ParseFailureException;
}
