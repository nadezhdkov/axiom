package io.axiom.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * General-purpose asynchronous execution utility backed by virtual threads (Java 21).
 *
 * <p>Unlike the pooled/bounded executors typically used for CPU-bound work, virtual threads are
 * cheap and unbounded by design — {@link #executor()} returns a shared
 * {@linkplain Executors#newVirtualThreadPerTaskExecutor() virtual-thread-per-task executor}
 * appropriate for I/O-bound or blocking-heavy workloads. It is also the default executor used by
 * {@link io.axiom.concurrent.promise.Promises#async}.
 *
 * <p>This is a standalone general-purpose utility, deliberately not part of an HTTP server or any
 * other framework-scale component (axiom.md §2 explicitly excludes those from Axiom's scope).
 */
public final class Tasks {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private Tasks() {
    }

    /** Shared virtual-thread-per-task executor. Never shut down by this class. */
    public static ExecutorService executor() {
        return EXECUTOR;
    }
}
