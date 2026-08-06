package io.axiom.concurrent.promise;

import io.axiom.concurrent.promise.internal.RetryPolicyBuilder;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Policy for retrying a failed {@link Promise}: how many attempts, what backoff strategy between
 * attempts, and which errors should trigger a retry at all.
 */
public interface RetryPolicy {

    /** Max retry attempts, not including the initial attempt. */
    int maxAttempts();

    /** Delay before the given (1-based) retry attempt. */
    Duration backoff(int attempt, Throwable lastError);

    boolean shouldRetry(Throwable error);

    static RetryPolicyConfigurer configure() {
        return new RetryPolicyBuilder();
    }

    static RetryPolicy simple(int maxAttempts) {
        return configure().maxAttempts(maxAttempts).build();
    }

    static RetryPolicy exponential(int maxAttempts, Duration initialDelay) {
        return configure().maxAttempts(maxAttempts).exponentialBackoff(initialDelay).build();
    }

    interface RetryPolicyConfigurer {
        RetryPolicyConfigurer maxAttempts(int maxAttempts);

        RetryPolicyConfigurer fixedDelay(Duration delay);

        /** Each retry doubles the delay. */
        RetryPolicyConfigurer exponentialBackoff(Duration initialDelay);

        RetryPolicyConfigurer exponentialBackoff(Duration initialDelay, double multiplier);

        RetryPolicyConfigurer maxDelay(Duration maxDelay);

        /** Adds jitter to backoff delays to avoid thundering herd. */
        RetryPolicyConfigurer withJitter();

        RetryPolicyConfigurer retryIf(Predicate<Throwable> predicate);

        @SuppressWarnings("unchecked")
        RetryPolicyConfigurer retryOn(Class<? extends Throwable>... errorTypes);

        RetryPolicy build();
    }
}
