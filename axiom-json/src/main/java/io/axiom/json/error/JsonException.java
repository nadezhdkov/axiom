package io.axiom.json.error;

/** Base of the {@code axiom-json} exception hierarchy. */
public sealed class JsonException extends RuntimeException
        permits JsonIoException, JsonMappingException, JsonParseException, JsonValidationException {

    private final JsonPath path;

    public JsonException(String message) {
        this(message, null, null);
    }

    public JsonException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public JsonException(String message, Throwable cause, JsonPath path) {
        super(format(message, path), cause);
        this.path = path;
    }

    private static String format(String message, JsonPath path) {
        return path != null && !path.isEmpty() ? message + " at path: " + path : message;
    }

    public JsonPath getPath() {
        return path;
    }

    public boolean hasPath() {
        return path != null && !path.isEmpty();
    }
}
