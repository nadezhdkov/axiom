package io.axiom.console.error;

/** Classifies why a non-throwing scan attempt failed. */
public enum ErrorCode {
    EOF,
    IO_ERROR,
    PARSE_ERROR,
    VALIDATION_ERROR
}
