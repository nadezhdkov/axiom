package io.axiom.console.error;

/** The error side of {@code Result<T, ScanError>} returned by {@code InputHandler#tryRead}. */
public record ScanError(ErrorCode code, String message, String rawInput, Throwable cause) {

    public static ScanError of(ErrorCode code, String message, String rawInput, Throwable cause) {
        return new ScanError(code, message, rawInput, cause);
    }

    public String pretty() {
        return "[" + code + "] " + message + (rawInput == null ? "" : " (input: \"" + rawInput + "\")");
    }

    @Override
    public String toString() {
        return pretty();
    }
}
