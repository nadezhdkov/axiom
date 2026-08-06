package io.axiom.core;

/**
 * Unchecked root exception for domain-specific exceptions raised by Axiom modules.
 *
 * <p>Individual modules are expected to subclass this rather than throw it directly, following
 * the per-domain exception hierarchy pattern that proved effective in both audited reference
 * libraries.
 */
public class AxiomException extends RuntimeException {

    public AxiomException(String message) {
        super(message);
    }

    public AxiomException(String message, Throwable cause) {
        super(message, cause);
    }

    public AxiomException(Throwable cause) {
        super(cause);
    }
}
