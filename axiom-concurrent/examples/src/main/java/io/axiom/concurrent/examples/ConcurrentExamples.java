package io.axiom.concurrent.examples;

import io.axiom.concurrent.box.Box;
import io.axiom.concurrent.promise.Promise;
import io.axiom.concurrent.promise.Promises;
import io.axiom.concurrent.promise.RetryPolicy;

import java.time.Duration;

/** Minimal, compiled-by-CI usage examples for {@code axiom-concurrent}. */
public final class ConcurrentExamples {

    private ConcurrentExamples() {
    }

    public static void main(String[] args) throws Throwable {
        // Promise — async computation with retry on failure
        Promise<Integer> loaded = Promises.async(ConcurrentExamples::flakyLoad)
                .retry(RetryPolicy.exponential(3, Duration.ofMillis(10)));
        System.out.println("Promise result: " + loaded.get(Duration.ofSeconds(5)));

        // Box — thread-safe counter
        Box<Integer> counter = Box.of(0);
        counter.updateAndGet(n -> n + 1);
        System.out.println("Box value: " + counter.get());
    }

    private static int attempt = 0;

    private static int flakyLoad() {
        attempt++;
        if (attempt < 2) {
            throw new RuntimeException("transient failure");
        }
        return 42;
    }
}
