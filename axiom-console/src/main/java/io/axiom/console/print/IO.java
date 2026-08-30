package io.axiom.console.print;

import io.axiom.console.print.internal.Placeholders;
import io.axiom.console.print.internal.TagRenderer;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced console output: {@code {}} positional placeholders (resolved first) plus
 * {@code [tag]...[/]} tags for color, background, and text style (resolved after, so arg values
 * can never inject a tag). Tags nest to combine: {@code [bold][green]...[/][/]}.
 *
 * <pre>{@code
 * IO.println("Hello {}, you have [bold][green]{}[/][/] new messages", name, count);
 * }</pre>
 *
 * @see Color
 * @see TextStyle
 */
public final class IO {

    private static final AtomicReference<PrintStream> OUT = new AtomicReference<>(System.out);
    private static final AtomicReference<Boolean> STYLING_ENABLED = new AtomicReference<>(null);

    private IO() {
    }

    public static void print(String template, Object... args) {
        OUT.get().print(render(template, args));
    }

    public static void println(String template, Object... args) {
        OUT.get().println(render(template, args));
    }

    /** Resolves placeholders and tags without writing anywhere — useful for building strings. */
    public static String render(String template, Object... args) {
        String substituted = Placeholders.substitute(template, args);
        String styled = TagRenderer.render(substituted, stylingEnabled());
        return Placeholders.unescapeBrackets(styled);
    }

    /** Swaps the target stream (default {@code System.out}); {@code null} is ignored. */
    public static void use(PrintStream stream) {
        if (stream != null) {
            OUT.set(stream);
        }
    }

    /** Forces styling on/off; {@code null} resets to auto-detection (TTY present and {@code NO_COLOR} unset). */
    public static void setStylingEnabled(Boolean enabled) {
        STYLING_ENABLED.set(enabled);
    }

    private static boolean stylingEnabled() {
        Boolean forced = STYLING_ENABLED.get();
        if (forced != null) {
            return forced;
        }
        return System.console() != null && System.getenv("NO_COLOR") == null;
    }
}
