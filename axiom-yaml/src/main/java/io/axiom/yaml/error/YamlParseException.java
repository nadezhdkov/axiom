package io.axiom.yaml.error;

/** Raised when the input text is not syntactically valid YAML. */
public final class YamlParseException extends YamlException {

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlParseException(String message, Throwable cause, YamlPath path) {
        super(message, cause, path);
    }
}
