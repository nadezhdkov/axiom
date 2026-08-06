package io.axiom.console;

import io.axiom.console.prompt.Messages;
import io.axiom.console.prompt.Prompt;
import io.axiom.console.prompt.Prompts;

import java.io.PrintStream;
import java.util.Locale;

public record PromptEnvironment(Locale locale, PrintStream out, PrintStream err, Prompt prompt, Messages messages) {

    public static PromptEnvironment defaults() {
        return new PromptEnvironment(Locale.getDefault(), System.out, System.err, Prompts.defaultPrompt(), Messages.defaults());
    }
}
