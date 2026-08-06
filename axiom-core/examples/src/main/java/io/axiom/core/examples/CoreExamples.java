package io.axiom.core.examples;

import io.axiom.core.result.Maybe;
import io.axiom.core.result.Result;
import io.axiom.core.result.Try;

/**
 * Minimal, compiled-by-CI usage examples for {@code axiom-core}. Kept in sync with the snippet
 * in {@code axiom-core/README.md} — see axiom.md §12 (examples must compile, never drift).
 */
public final class CoreExamples {

    private CoreExamples() {
    }

    public static void main(String[] args) {
        // Try — capture of exceptions thrown by a Java API
        Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"))
                .map(n -> n * 2)
                .recover(NumberFormatException.class, ex -> 0);
        System.out.println("Try: " + parsed.getOrElse(-1));

        // Result — expected, typed domain failure
        Result<Integer, String> validated = validateAge(17);
        String message = validated.fold(
                age -> "accepted: " + age,
                error -> "rejected: " + error
        );
        System.out.println("Result: " + message);

        // Maybe — absence without an associated cause
        Maybe<String> nickname = Maybe.<String>none();
        String display = nickname.orElse("anonymous");
        System.out.println("Maybe: " + display);
    }

    private static Result<Integer, String> validateAge(int age) {
        return age >= 18 ? Result.ok(age) : Result.err("must be 18 or older");
    }
}
