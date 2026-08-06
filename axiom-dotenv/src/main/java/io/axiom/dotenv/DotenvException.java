package io.axiom.dotenv;

import io.axiom.core.AxiomException;

public class DotenvException extends AxiomException {

    public DotenvException(String message) {
        super(message);
    }

    public DotenvException(String message, Throwable cause) {
        super(message, cause);
    }
}
