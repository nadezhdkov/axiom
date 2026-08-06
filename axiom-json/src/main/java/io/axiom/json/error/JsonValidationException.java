package io.axiom.json.error;

/** Raised when {@code @JsonRequired} enforcement fails during decoding. */
public final class JsonValidationException extends JsonException {

    public JsonValidationException(String message) {
        super(message);
    }

    public JsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonValidationException(String message, Throwable cause, JsonPath path) {
        super(message, cause, path);
    }
}
