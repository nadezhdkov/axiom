package io.axiom.concurrent.promise.internal;

import io.axiom.concurrent.Tasks;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Scheduling and default-execution services for {@link io.axiom.concurrent.promise.Promise}.
 *
 * <p>The default executor is {@link Tasks#executor()} (virtual-thread-per-task), not the
 * platform-thread {@code ForkJoinPool.commonPool()} used by the reference implementation this
 * was ported from — axiom.md §7 treats using virtual threads where they solve a real problem
 * (unbounded, cheap concurrency for I/O-bound async work) as a design principle, not an
 * afterthought. Delayed/scheduled work still uses a small daemon-thread pool, since the JDK has
 * no scheduled executor backed by virtual threads.
 */
public final class PromiseScheduler {

    private static final PromiseScheduler INSTANCE = new PromiseScheduler();

    private final ScheduledExecutorService scheduler;

    private PromiseScheduler() {
        this.scheduler = createScheduler();
    }

    public static PromiseScheduler getInstance() {
        return INSTANCE;
    }

    public Executor defaultExecutor() {
        return Tasks.executor();
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    public ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        return scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    public <T> ScheduledFuture<T> schedule(Callable<T> callable, Duration delay) {
        return scheduler.schedule(callable, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static ScheduledExecutorService createScheduler() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(2, processors / 2);

        return new ScheduledThreadPoolExecutor(corePoolSize, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "axiom-promise-scheduler-" + counter++);
                thread.setDaemon(true);
                return thread;
            }
        });
    }
}
