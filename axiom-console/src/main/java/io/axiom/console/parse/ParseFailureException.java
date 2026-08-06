package io.axiom.console.parse;

public class ParseFailureException extends RuntimeException {

    public ParseFailureException(String message) {
        super(message);
    }

    public ParseFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
