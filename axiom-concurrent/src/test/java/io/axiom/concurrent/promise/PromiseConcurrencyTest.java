package io.axiom.concurrent.promise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real multi-thread concurrency tests for {@link Promise} — axiom.md §11 names this explicitly
 * as the largest confirmed risk of the original {@code obsidian-promise} module, which shipped
 * with zero tests at all. These exercise many threads racing to resolve/reject/cancel the same
 * {@link Deferred} simultaneously and assert exactly one outcome wins.
 */
class PromiseConcurrencyTest {

    @Test
    @Timeout(30)
    void manyThreadsRacingToResolveTheSameDeferredOnlyOneWins() throws InterruptedException {
        int threads = 50;
        Deferred<Integer> deferred = Promises.defer();
        Promise<Integer> promise = deferred.promise();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successfulResolves = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            int value = i;
            pool.submit(() -> {
                try {
                    start.await();
                    if (deferred.resolve(value)) {
                        successfulResolves.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1, successfulResolves.get());
        assertTrue(promise.isFulfilled());
    }

    @Test
    @Timeout(30)
    void concurrentResolveAndCancelRaceLeavesExactlyOneWinner() throws InterruptedException {
        int racers = 40;
        Deferred<String> deferred = Promises.defer();
        Promise<String> promise = deferred.promise();

        ExecutorService pool = Executors.newFixedThreadPool(racers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(racers);
        AtomicInteger wins = new AtomicInteger();

        for (int i = 0; i < racers; i++) {
            boolean tryResolve = i % 2 == 0;
            pool.submit(() -> {
                try {
                    start.await();
                    boolean won = tryResolve ? deferred.resolve("resolved") : deferred.cancel("cancelled");
                    if (won) wins.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1, wins.get());
        assertTrue(promise.isFulfilled() || promise.isCancelled());
    }

    @Test
    @Timeout(30)
    void manyConcurrentAsyncPromisesAllCompleteCorrectly() throws Throwable {
        int count = 200;
        java.util.List<Promise<Integer>> promises = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            int value = i;
            promises.add(Promises.async(() -> value * 2));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.List<Promise<? extends Integer>> widened = (java.util.List) promises;
        java.util.List<Integer> results = Promises.all(widened).get(java.time.Duration.ofSeconds(20));

        assertEquals(count, results.size());
        for (int i = 0; i < count; i++) {
            assertEquals(i * 2, results.get(i));
        }
    }

    @Test
    @Timeout(30)
    void cancellationTokenCallbacksAreDeliveredExactlyOnceUnderConcurrentCancelAttempts()
            throws InterruptedException {
        CancellationSource source = CancellationSource.create();
        int listeners = 30;
        CountDownLatch allCalled = new CountDownLatch(listeners);
        AtomicInteger callbackInvocations = new AtomicInteger();

        for (int i = 0; i < listeners; i++) {
            source.token().onCancelled(() -> {
                callbackInvocations.incrementAndGet();
                allCalled.countDown();
            });
        }

        int cancellers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(cancellers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(cancellers);

        for (int i = 0; i < cancellers; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    source.cancel("racing cancel");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(allCalled.await(10, TimeUnit.SECONDS));
        assertEquals(listeners, callbackInvocations.get());
        assertTrue(source.isCancelled());
    }
}
