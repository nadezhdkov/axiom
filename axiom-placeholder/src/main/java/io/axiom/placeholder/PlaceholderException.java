package io.axiom.placeholder;

import io.axiom.core.AxiomException;

/** Root unchecked exception for placeholder resolution errors. */
public class PlaceholderException extends AxiomException {

    public PlaceholderException(String message) {
        super(message);
    }
}
