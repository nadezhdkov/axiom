package io.axiom.json.error;

/** Raised when reading/writing the underlying source or sink of a JSON document fails. */
public final class JsonIoException extends JsonException {

    public JsonIoException(String message) {
        super(message);
    }

    public JsonIoException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonIoException(String message, Throwable cause, JsonPath path) {
        super(message, cause, path);
    }
}
