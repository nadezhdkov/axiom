package io.axiom.placeholder;

/** Thrown when a {@code ${key}} placeholder has no default and no source resolves {@code key}. */
public class UnresolvedPlaceholderException extends PlaceholderException {

    public UnresolvedPlaceholderException(String key) {
        super("Unresolved placeholder: no value for key \"" + key + "\" and no default given");
    }
}
