package io.axiom.console.prompt;

/** Formats a prompt label into the text actually printed before reading a line. */
@FunctionalInterface
public interface Prompt {
    String format(String label);
}
