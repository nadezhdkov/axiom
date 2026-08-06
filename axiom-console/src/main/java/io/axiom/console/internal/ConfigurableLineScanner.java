package io.axiom.console.internal;

import io.axiom.console.InputHandler;
import io.axiom.console.PromptEnvironment;
import io.axiom.console.error.ErrorCode;
import io.axiom.console.error.ScanError;
import io.axiom.console.parse.ParseFailureException;
import io.axiom.console.parse.Parser;
import io.axiom.console.source.InputSource;
import io.axiom.console.validate.ValidationException;
import io.axiom.console.validate.Validator;
import io.axiom.core.result.Result;

import java.io.IOException;

public final class ConfigurableLineScanner implements InputHandler {

    private final InputSource source;
    private final PromptEnvironment config;

    public ConfigurableLineScanner(InputSource source, PromptEnvironment config) {
        this.source = source;
        this.config = config == null ? PromptEnvironment.defaults() : config;
    }

    @Override
    public boolean hasNextLine() {
        try {
            source.reader().mark(1);
            int next = source.reader().read();
            source.reader().reset();
            return next != -1;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String line() {
        return line("");
    }

    @Override
    public String line(String prompt) {
        printPrompt(prompt);
        try {
            String raw = source.reader().readLine();
            return raw == null ? "" : raw;
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public <T> T read(Parser<T> parser) {
        return read("", parser);
    }

    @Override
    public <T> T read(String prompt, Parser<T> parser) {
        return parser.parse(line(prompt));
    }

    @Override
    public <T> T until(String prompt, Parser<T> parser, Validator<T> validator) {
        while (true) {
            String raw = line(prompt);
            T value;
            try {
                value = parser.parse(raw);
            } catch (RuntimeException e) {
                config.out().println(config.messages().invalidEntry());
                continue;
            }
            try {
                validator.validate(value);
            } catch (ValidationException e) {
                config.out().println(config.messages().invalidValue());
                continue;
            }
            return value;
        }
    }

    @Override
    public <T> Result<T, ScanError> tryRead(Parser<T> parser) {
        return tryRead("", parser, v -> {
        });
    }

    @Override
    public <T> Result<T, ScanError> tryRead(String prompt, Parser<T> parser) {
        return tryRead(prompt, parser, v -> {
        });
    }

    @Override
    public <T> Result<T, ScanError> tryRead(String prompt, Parser<T> parser, Validator<T> validator) {
        if (!hasNextLine()) {
            return Result.err(ScanError.of(ErrorCode.EOF, "No more input available", null, null));
        }
        String raw = line(prompt);
        T value;
        try {
            value = parser.parse(raw);
        } catch (ParseFailureException e) {
            return Result.err(ScanError.of(ErrorCode.PARSE_ERROR, e.getMessage(), raw, e));
        }
        try {
            validator.validate(value);
        } catch (ValidationException e) {
            return Result.err(ScanError.of(ErrorCode.VALIDATION_ERROR, e.getMessage(), raw, e));
        }
        return Result.ok(value);
    }

    private void printPrompt(String label) {
        String formatted = config.prompt().format(label);
        if (!formatted.isEmpty()) {
            config.out().print(formatted);
        }
    }

    @Override
    public void close() {
        source.close();
    }
}
