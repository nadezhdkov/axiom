package io.axiom.concurrent.promise.internal.backoff;

import io.axiom.concurrent.promise.BackoffStrategy;

import java.time.Duration;

public class FixedBackoff implements BackoffStrategy {
    private final Duration delay;

    public FixedBackoff(Duration delay) {
        this.delay = delay;
    }

    @Override
    public Duration calculateDelay(int attempt) {
        return delay;
    }

    @Override
    public BackoffStrategy withMaxDelay(Duration maxDelay) {
        return delay.compareTo(maxDelay) > 0 ? new FixedBackoff(maxDelay) : this;
    }

    @Override
    public BackoffStrategy withJitter() {
        return new JitteredBackoff(this);
    }
}
