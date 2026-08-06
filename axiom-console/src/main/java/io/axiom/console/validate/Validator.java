package io.axiom.console.validate;

/** Validates a successfully parsed value, or throws {@link ValidationException}. */
@FunctionalInterface
public interface Validator<T> {
    void validate(T value) throws ValidationException;
}
