package io.axiom.json.error;

/** Raised when converting between a {@code JsonElement} tree and a Java object fails. */
public final class JsonMappingException extends JsonException {

    public JsonMappingException(String message) {
        super(message);
    }

    public JsonMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonMappingException(String message, Throwable cause, JsonPath path) {
        super(message, cause, path);
    }
}
