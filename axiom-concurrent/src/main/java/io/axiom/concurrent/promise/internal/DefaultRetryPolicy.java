package io.axiom.concurrent.promise.internal;

import io.axiom.concurrent.promise.BackoffStrategy;
import io.axiom.concurrent.promise.RetryPolicy;

import java.time.Duration;
import java.util.function.Predicate;

public class DefaultRetryPolicy implements RetryPolicy {

    private final int maxAttempts;
    private final BackoffStrategy backoffStrategy;
    private final Predicate<Throwable> retryPredicate;

    DefaultRetryPolicy(int maxAttempts, BackoffStrategy backoffStrategy, Predicate<Throwable> retryPredicate) {
        this.maxAttempts = maxAttempts;
        this.backoffStrategy = backoffStrategy;
        this.retryPredicate = retryPredicate;
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }

    @Override
    public Duration backoff(int attempt, Throwable lastError) {
        return backoffStrategy.calculateDelay(attempt);
    }

    @Override
    public boolean shouldRetry(Throwable error) {
        return retryPredicate.test(error);
    }
}
