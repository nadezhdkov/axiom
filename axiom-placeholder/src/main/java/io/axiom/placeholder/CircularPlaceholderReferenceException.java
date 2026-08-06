package io.axiom.placeholder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when resolving a placeholder key would recurse back into a key already being resolved
 * (e.g. {@code ${a}} &rarr; {@code ${b}} &rarr; {@code ${a}}). Detection is mandatory (see
 * package documentation) — without it, a cycle in the underlying source becomes a
 * {@link StackOverflowError} instead of a clear, catchable error.
 */
public class CircularPlaceholderReferenceException extends PlaceholderException {

    public CircularPlaceholderReferenceException(List<String> cycle) {
        super("Circular placeholder reference detected: "
                + cycle.stream().collect(Collectors.joining(" -> ")));
    }
}
