package io.axiom.core.result;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TryTest {

    @Test
    void ofCapturesSuccess() {
        Try<Integer> t = Try.of(() -> Integer.parseInt("42"));
        assertTrue(t.isSuccess());
        assertEquals(42, t.getOrNull());
    }

    @Test
    void ofCapturesFailure() {
        Try<Integer> t = Try.of(() -> Integer.parseInt("not a number"));
        assertTrue(t.isFailure());
        assertTrue(t.getFailureOrNull() instanceof NumberFormatException);
    }

    @Test
    void mapAndFlatMapChainOnSuccess() {
        Try<Integer> result = Try.of(() -> "10")
                .map(Integer::parseInt)
                .flatMap(n -> Try.of(() -> n * 2));

        assertTrue(result.isSuccess());
        assertEquals(20, result.getOrNull());
    }

    @Test
    void mapShortCircuitsOnFailure() {
        Try<Integer> result = Try.<Integer>failure(new IllegalStateException("boom"))
                .map(n -> n * 2);

        assertTrue(result.isFailure());
        assertTrue(result.getFailureOrNull() instanceof IllegalStateException);
    }

    @Test
    void recoverAfterMapReturnsToSuccess() {
        int value = Try.of(() -> Integer.parseInt("x"))
                .recover(NumberFormatException.class, ex -> 0)
                .map(n -> n + 1)
                .getOrElse(-1);

        assertEquals(1, value);
    }

    @Test
    void typedRecoverIgnoresUnrelatedExceptionType() {
        Try<Integer> t = Try.<Integer>failure(new IllegalStateException("boom"))
                .recover(NumberFormatException.class, ex -> 0);

        assertTrue(t.isFailure());
    }

    @Test
    void staticRetryReinvokesOriginalSupplierEachAttempt() {
        // Regression test: a previous instance-level Try.retry(int) implementation replayed
        // the already-computed value instead of re-invoking the original supplier, so it never
        // actually retried the failing operation. The static overload must re-invoke the
        // supplier on every attempt.
        AtomicInteger attempts = new AtomicInteger();
        Try<String> result = Try.retry(3, Duration.ofMillis(1), () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("fails on attempt " + attempt);
            }
            return "ok on attempt " + attempt;
        });

        assertEquals(3, attempts.get());
        assertTrue(result.isSuccess());
        assertEquals("ok on attempt 3", result.getOrNull());
    }

    @Test
    void staticRetryGivesUpAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        Try<String> result = Try.retry(2, Duration.ofMillis(1), () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("always fails");
        });

        assertEquals(2, attempts.get());
        assertTrue(result.isFailure());
    }

    @Test
    void retryWithBackoffReinvokesSupplier() {
        AtomicInteger attempts = new AtomicInteger();
        Try<Integer> result = Try.retryWithBackoff(4, Duration.ofMillis(1), () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("not yet");
            }
            return attempts.get();
        });

        assertTrue(result.isSuccess());
        assertEquals(2, result.getOrNull());
    }

    @Test
    void sequenceFailsFastOnFirstFailure() {
        var tries = java.util.List.of(
                Try.success(1),
                Try.<Integer>failure(new RuntimeException("bad")),
                Try.success(3)
        );

        Try<java.util.List<Integer>> result = Try.sequence(tries);
        assertTrue(result.isFailure());
    }

    @Test
    void sequenceSucceedsWhenAllSucceed() {
        var tries = java.util.List.of(Try.success(1), Try.success(2), Try.success(3));
        Try<java.util.List<Integer>> result = Try.sequence(tries);

        assertTrue(result.isSuccess());
        assertEquals(java.util.List.of(1, 2, 3), result.getOrNull());
    }

    @Test
    void toOptionalReflectsSuccessAndFailure() {
        assertTrue(Try.success("x").toOptional().isPresent());
        assertFalse(Try.<String>failure(new RuntimeException()).toOptional().isPresent());
    }

    @Test
    void orThrowRethrowsOriginalException() {
        RuntimeException original = new IllegalArgumentException("bad input");
        Try<String> t = Try.failure(original);

        RuntimeException thrown = assertThrows(IllegalArgumentException.class, t::orThrow);
        assertEquals(original, thrown);
    }

    @Test
    void checkedGetRethrowsCheckedException() {
        Exception original = new java.io.IOException("disk error");
        Try<String> t = Try.failure(original);

        Exception thrown = assertThrows(java.io.IOException.class, t::checkedGet);
        assertEquals(original, thrown);
    }
}
