package io.axiom.csv;

/**
 * Root exception for {@code axiom-csv}. Extends {@link RuntimeException} directly rather than
 * {@code axiom-core.AxiomException} — this module deliberately has no dependency on
 * {@code axiom-core} (see {@code docs/CONVENTIONS.md}, "Hierarquia de exceções").
 */
public final class CsvException extends RuntimeException {

    public CsvException(String message) {
        super(message);
    }

    public CsvException(String message, Throwable cause) {
        super(message, cause);
    }
}
