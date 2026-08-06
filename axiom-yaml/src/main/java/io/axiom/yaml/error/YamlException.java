package io.axiom.yaml.error;

/** Base of the {@code axiom-yaml} exception hierarchy. */
public sealed class YamlException extends RuntimeException
        permits YamlIoException, YamlMappingException, YamlParseException, YamlValidationException {

    private final YamlPath path;

    public YamlException(String message) {
        this(message, null, null);
    }

    public YamlException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public YamlException(String message, Throwable cause, YamlPath path) {
        super(format(message, path), cause);
        this.path = path;
    }

    private static String format(String message, YamlPath path) {
        return path != null && !path.isEmpty() ? message + " at path: " + path : message;
    }

    public YamlPath getPath() {
        return path;
    }

    public boolean hasPath() {
        return path != null && !path.isEmpty();
    }
}
