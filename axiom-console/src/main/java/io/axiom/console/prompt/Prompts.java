package io.axiom.console.prompt;

public final class Prompts {

    private Prompts() {
    }

    public static Prompt defaultPrompt() {
        return label -> label == null || label.isBlank() ? "" : label + "> ";
    }
}
