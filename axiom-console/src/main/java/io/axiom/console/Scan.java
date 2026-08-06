package io.axiom.console;

import io.axiom.console.parse.Parsers;
import io.axiom.console.validate.Validators;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Static convenience facade over a swappable default {@link InputHandler}.
 *
 * <p>The default engine is held in an {@link AtomicReference}, not a bare mutable field, so
 * {@link #use} is safe under concurrent access — the one static mutable state this module keeps
 * is deliberately synchronized, per {@code axiom.md}'s "no unsynchronized static mutable state"
 * principle. Prefer {@link InputScanner} directly when you want an explicit, non-static instance.
 */
public final class Scan {

    private static final AtomicReference<InputHandler> DEFAULT = new AtomicReference<>(InputScanner.console());

    private Scan() {
    }

    public static void use(InputHandler engine) {
        if (engine != null) {
            DEFAULT.set(engine);
        }
    }

    public static String line() {
        return DEFAULT.get().line();
    }

    public static String line(String prompt) {
        return DEFAULT.get().line(prompt);
    }

    public static int i32(String prompt) {
        return DEFAULT.get().read(prompt, Parsers.i32());
    }

    public static long i64(String prompt) {
        return DEFAULT.get().read(prompt, Parsers.i64());
    }

    public static double f64(String prompt) {
        return DEFAULT.get().read(prompt, Parsers.f64());
    }

    public static boolean bool(String prompt) {
        return DEFAULT.get().read(prompt, Parsers.bool());
    }

    public static String str(String prompt) {
        return DEFAULT.get().read(prompt, Parsers.string());
    }

    public static <T> T until(String prompt, io.axiom.console.parse.Parser<T> parser, io.axiom.console.validate.Validator<T> validator) {
        return DEFAULT.get().until(prompt, parser, validator);
    }

    public static String notBlank(String prompt) {
        return DEFAULT.get().until(prompt, Parsers.string(), Validators.notBlank());
    }

    public static void close() {
        DEFAULT.get().close();
    }
}
