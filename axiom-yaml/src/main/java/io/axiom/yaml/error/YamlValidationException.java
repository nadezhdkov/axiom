package io.axiom.yaml.error;

/** Raised when {@code @YamlRequired} enforcement fails during decoding. */
public final class YamlValidationException extends YamlException {

    public YamlValidationException(String message) {
        super(message);
    }

    public YamlValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlValidationException(String message, Throwable cause, YamlPath path) {
        super(message, cause, path);
    }
}
