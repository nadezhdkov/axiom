package io.axiom.yaml.error;

/** Raised when reading/writing the underlying source or sink of a YAML document fails. */
public final class YamlIoException extends YamlException {

    public YamlIoException(String message) {
        super(message);
    }

    public YamlIoException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlIoException(String message, Throwable cause, YamlPath path) {
        super(message, cause, path);
    }
}
