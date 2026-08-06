package io.axiom.console;

import io.axiom.console.error.ScanError;
import io.axiom.console.parse.Parser;
import io.axiom.console.validate.Validator;
import io.axiom.core.result.Result;

/**
 * A console-style read session over some {@link io.axiom.console.source.InputSource}.
 *
 * <p>{@code read}/{@code until} are the interactive shape: they throw or loop-and-reprompt.
 * {@code tryRead} is the non-throwing shape, returning {@code Result<T, ScanError>} — this closes
 * the gap left in the Obsidian module this was ported from, where an equivalent
 * {@code ScanResult}/{@code ErrorCode} pair existed but was never actually wired into the read
 * path (every read there still threw raw exceptions internally).
 */
public interface InputHandler extends AutoCloseable {

    boolean hasNextLine();

    String line();

    String line(String prompt);

    <T> T read(Parser<T> parser);

    <T> T read(String prompt, Parser<T> parser);

    /** Reprompts on parse/validation failure until a valid value is read. */
    <T> T until(String prompt, Parser<T> parser, Validator<T> validator);

    <T> Result<T, ScanError> tryRead(Parser<T> parser);

    <T> Result<T, ScanError> tryRead(String prompt, Parser<T> parser);

    <T> Result<T, ScanError> tryRead(String prompt, Parser<T> parser, Validator<T> validator);

    @Override
    void close();
}
