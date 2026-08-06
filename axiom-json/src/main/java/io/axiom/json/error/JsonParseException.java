package io.axiom.json.error;

/** Raised when the input text is not syntactically valid JSON. */
public final class JsonParseException extends JsonException {

    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonParseException(String message, Throwable cause, JsonPath path) {
        super(message, cause, path);
    }
}
