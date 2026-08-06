package io.axiom.console;

import io.axiom.console.internal.ConfigurableLineScanner;
import io.axiom.console.source.ConsoleSource;
import io.axiom.console.source.ReaderSource;
import io.axiom.console.source.StringSource;

import java.io.Reader;

/** Stateless factory for {@link InputHandler}s over each {@link io.axiom.console.source.InputSource}. */
public final class InputScanner {

    private InputScanner() {
    }

    public static InputHandler console() {
        return console(PromptEnvironment.defaults());
    }

    public static InputHandler console(PromptEnvironment config) {
        return new ConfigurableLineScanner(new ConsoleSource(), config);
    }

    public static InputHandler fromString(String content) {
        return new ConfigurableLineScanner(new StringSource(content), PromptEnvironment.defaults());
    }

    public static InputHandler fromReader(Reader reader) {
        return new ConfigurableLineScanner(new ReaderSource(reader), PromptEnvironment.defaults());
    }
}
