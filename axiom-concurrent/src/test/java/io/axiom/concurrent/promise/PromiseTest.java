package io.axiom.concurrent.promise;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromiseTest {

    @Test
    void resolvedPromiseIsFulfilled() throws Throwable {
        Promise<Integer> p = Promises.value(42);
        assertTrue(p.isFulfilled());
        assertEquals(42, p.get());
    }

    @Test
    void rejectedPromiseIsRejected() {
        Promise<Integer> p = Promises.error(new IllegalStateException("boom"));
        assertTrue(p.isRejected());
        assertThrows(IllegalStateException.class, p::get);
    }

    @Test
    void mapTransformsSuccessValue() throws Throwable {
        Promise<Integer> p = Promises.value(10).map(n -> n * 2);
        assertEquals(20, p.get());
    }

    @Test
    void flatMapChainsAsyncOperations() throws Throwable {
        Promise<Integer> p = Promises.value(10).flatMap(n -> Promises.value(n + 5));
        assertEquals(15, p.get());
    }

    @Test
    void recoverConvertsFailureIntoSuccess() throws Throwable {
        Promise<Integer> p = Promises.<Integer>error(new RuntimeException("fail")).recover(err -> -1);
        assertEquals(-1, p.get());
    }

    @Test
    void asyncSupplierRunsAndResolves() throws Throwable {
        Promise<String> p = Promises.async(() -> "computed");
        assertEquals("computed", p.get(Duration.ofSeconds(5)));
    }

    @Test
    void asyncSupplierExceptionRejectsPromise() {
        Promise<String> p = Promises.async(() -> {
            throw new IllegalArgumentException("bad input");
        });
        Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
            try {
                p.get(Duration.ofSeconds(5));
            } catch (Throwable t) {
                throw t;
            }
        });
        assertEquals("bad input", thrown.getMessage());
    }

    @Test
    void retryReinvokesOriginalSupplierUntilSuccess() throws Throwable {
        // Regression test: a previous Promise.retry(policy) implementation recovered against the
        // same already-completed future on every attempt instead of re-running the original
        // supplier — the same bug class fixed in axiom-core's Try.retry.
        AtomicInteger attempts = new AtomicInteger();

        Promise<String> p = Promises.async(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("fails on attempt " + attempt);
            }
            return "ok on attempt " + attempt;
        }).retry(RetryPolicy.simple(5));

        assertEquals("ok on attempt 3", p.get(Duration.ofSeconds(5)));
        assertEquals(3, attempts.get());
    }

    @Test
    void retryGivesUpAfterMaxAttemptsAndPreservesAttemptCount() {
        AtomicInteger attempts = new AtomicInteger();

        Promise<String> p = Promises.<String>async(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("always fails");
        }).retry(RetryPolicy.simple(3));

        assertThrows(RuntimeException.class, () -> {
            try {
                p.get(Duration.ofSeconds(5));
            } catch (Throwable t) {
                throw t;
            }
        });
        // RetryPolicy.maxAttempts() is retries *not counting* the initial attempt (per its
        // javadoc), so simple(3) means 1 initial run + 3 retries = 4 total invocations.
        assertEquals(4, attempts.get());
    }

    @Test
    void deferredResolveCompletesPromise() throws Throwable {
        Deferred<String> deferred = Promises.defer();
        Promise<String> promise = deferred.promise();

        assertTrue(promise.isPending());
        assertTrue(deferred.resolve("done"));
        assertEquals("done", promise.get());
    }

    @Test
    void cancelMovesPromiseToCancelledState() {
        Deferred<String> deferred = Promises.defer();
        assertTrue(deferred.cancel("not needed anymore"));
        assertTrue(deferred.promise().isCancelled());
    }

    @Test
    void allResolvesWithResultsInOrder() throws Throwable {
        Promise<java.util.List<Integer>> all = Promises.all(
                Promises.value(1), Promises.value(2), Promises.value(3)
        );
        assertEquals(java.util.List.of(1, 2, 3), all.get(Duration.ofSeconds(5)));
    }

    @Test
    void allFailsFastOnFirstError() {
        Promise<java.util.List<Integer>> all = Promises.all(
                Promises.value(1), Promises.<Integer>error(new RuntimeException("bad")), Promises.value(3)
        );
        assertThrows(RuntimeException.class, () -> {
            try {
                all.get(Duration.ofSeconds(5));
            } catch (Throwable t) {
                throw t;
            }
        });
    }

    @Test
    void anyResolvesWithFirstSuccess() throws Throwable {
        Promise<Integer> any = Promises.any(
                Promises.<Integer>error(new RuntimeException("fail 1")), Promises.value(7)
        );
        assertEquals(7, any.get(Duration.ofSeconds(5)));
    }

    @Test
    void raceCompletesWithFirstResolvedValue() throws Throwable {
        Promise<Integer> race = Promises.race(Promises.value(1), Promises.value(2));
        Integer result = race.get(Duration.ofSeconds(5));
        assertTrue(result == 1 || result == 2);
    }

    @Test
    void timeoutRejectsPromiseThatNeverCompletes() {
        Promise<String> never = Promises.<String>never().timeout(Duration.ofMillis(50));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            try {
                never.get(Duration.ofSeconds(5));
            } catch (Throwable t) {
                throw t;
            }
        });
        assertTrue(thrown.getCause() instanceof io.axiom.concurrent.promise.error.TimeoutException);
    }
}
