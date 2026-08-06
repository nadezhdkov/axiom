package io.axiom.concurrent.promise;

import io.axiom.concurrent.promise.internal.backoff.ExponentialBackoff;
import io.axiom.concurrent.promise.internal.backoff.FixedBackoff;
import io.axiom.concurrent.promise.internal.backoff.NoBackoff;

import java.time.Duration;

/** Strategy for computing the delay before a {@link RetryPolicy} attempt. */
public interface BackoffStrategy {

    Duration calculateDelay(int attempt);

    BackoffStrategy withMaxDelay(Duration maxDelay);

    BackoffStrategy withJitter();

    static BackoffStrategy none() {
        return new NoBackoff();
    }

    static BackoffStrategy fixed(Duration delay) {
        return new FixedBackoff(delay);
    }

    static BackoffStrategy exponential(Duration initialDelay, double multiplier) {
        return new ExponentialBackoff(initialDelay, multiplier, null, false);
    }
}
