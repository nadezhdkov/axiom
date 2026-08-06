package io.axiom.console.validate;

public final class Validators {

    private Validators() {
    }

    public static <T> Validator<T> alwaysOk() {
        return value -> {
        };
    }

    public static Validator<String> notBlank() {
        return value -> {
            if (value == null || value.isBlank()) {
                throw new ValidationException("Value cannot be blank");
            }
        };
    }

    public static Validator<Integer> range(int min, int max) {
        return value -> {
            if (value < min || value > max) {
                throw new ValidationException("Expected range " + min + ".." + max);
            }
        };
    }
}
