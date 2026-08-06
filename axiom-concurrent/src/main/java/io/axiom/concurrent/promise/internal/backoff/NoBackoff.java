package io.axiom.concurrent.promise.internal.backoff;

import io.axiom.concurrent.promise.BackoffStrategy;

import java.time.Duration;

public class NoBackoff implements BackoffStrategy {

    @Override
    public Duration calculateDelay(int attempt) {
        return Duration.ZERO;
    }

    @Override
    public BackoffStrategy withMaxDelay(Duration maxDelay) {
        return this;
    }

    @Override
    public BackoffStrategy withJitter() {
        return this;
    }
}
