package io.axiom.yaml.error;

/** Raised when converting between a {@code YamlNode} tree and a Java object fails. */
public final class YamlMappingException extends YamlException {

    public YamlMappingException(String message) {
        super(message);
    }

    public YamlMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlMappingException(String message, Throwable cause, YamlPath path) {
        super(message, cause, path);
    }
}
