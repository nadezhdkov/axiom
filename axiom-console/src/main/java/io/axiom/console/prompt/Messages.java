package io.axiom.console.prompt;

public record Messages(String invalidEntry, String invalidValue) {

    public static Messages defaults() {
        return new Messages(
                "Invalid entry. Please try again.",
                "Value does not meet the criteria. Please try again."
        );
    }
}
