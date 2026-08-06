package io.axiom.concurrent.box;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {

    @Test
    void plainBoxGetSet() {
        Box<Integer> box = Box.plain(1);
        assertEquals(1, box.get());
        box.set(2);
        assertEquals(2, box.get());
    }

    @Test
    void plainBoxCompareAndSetUnsupported() {
        Box<Integer> box = Box.plain(1);
        assertThrows(UnsupportedOperationException.class, () -> box.compareAndSet(1, 2));
    }

    @Test
    void volatileBoxGetSet() {
        Box<Boolean> box = Box.volatileBox(false);
        assertFalse(box.get());
        box.set(true);
        assertTrue(box.get());
    }

    @Test
    void atomicBoxCompareAndSet() {
        Box<Integer> box = Box.of(1);
        assertTrue(box.compareAndSet(1, 2));
        assertEquals(2, box.get());
        assertFalse(box.compareAndSet(1, 3));
        assertEquals(2, box.get());
    }

    @Test
    void atomicBoxUpdateAndGet() {
        Box<Integer> box = Box.of(10);
        assertEquals(11, box.updateAndGet(n -> n + 1));
    }

    @Test
    void viewReflectsSourceAndRejectsSet() {
        Box<Integer> source = Box.of(10);
        Box<String> view = source.view(Object::toString);

        assertEquals("10", view.get());
        source.set(20);
        assertEquals("20", view.get());
        assertThrows(UnsupportedOperationException.class, () -> view.set("x"));
    }

    @Test
    void getOrElseAndIfPresent() {
        Box<String> box = Box.plain(null);
        assertEquals("default", box.getOrElse("default"));
        assertTrue(box.isNull());

        box.set("value");
        assertFalse(box.isNull());
        assertEquals("value", box.getOrElse("default"));
    }

    @Test
    void atomicBoxSurvivesConcurrentIncrements() throws InterruptedException {
        Box<Integer> counter = Box.of(0);
        int threads = 16;
        int incrementsPerThread = 1000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int i = 0; i < incrementsPerThread; i++) {
                        counter.updateAndGet(n -> n + 1);
                    }
                } catch (InterruptedException e) {
                    errors.incrementAndGet();
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(0, errors.get());
        assertEquals(threads * incrementsPerThread, counter.get());
    }
}
