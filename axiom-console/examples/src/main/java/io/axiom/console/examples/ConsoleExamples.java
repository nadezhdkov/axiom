package io.axiom.console.examples;

import io.axiom.console.InputHandler;
import io.axiom.console.InputScanner;
import io.axiom.console.error.ScanError;
import io.axiom.console.parse.Parsers;
import io.axiom.console.validate.Validators;
import io.axiom.core.result.Result;

/** Minimal, compiled-by-CI usage examples for {@code axiom-console}. */
public final class ConsoleExamples {

    private ConsoleExamples() {
    }

    public static void main(String[] args) {
        // Scripted input via StringSource — no System.in involved, same seam the test suite uses.
        InputHandler handler = InputScanner.fromString("Ada\n250\nnot-a-number\n");

        String name = handler.until("name", Parsers.string(), Validators.notBlank());
        System.out.println("name: " + name);

        int age = handler.until("age", Parsers.i32(), Validators.range(0, 130));
        System.out.println("age: " + age);

        Result<Integer, ScanError> maybeCount = handler.tryRead("count", Parsers.i32());
        maybeCount.fold(
                value -> {
                    System.out.println("count: " + value);
                    return null;
                },
                error -> {
                    System.out.println("count failed: " + error.pretty());
                    return null;
                }
        );

        handler.close();
    }
}
