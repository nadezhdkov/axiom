package io.axiom.concurrent.promise.internal.backoff;

import io.axiom.concurrent.promise.BackoffStrategy;

import java.time.Duration;

public class JitteredBackoff implements BackoffStrategy {
    private final BackoffStrategy delegate;

    public JitteredBackoff(BackoffStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public Duration calculateDelay(int attempt) {
        long millis = delegate.calculateDelay(attempt).toMillis();
        long jittered = (long) (millis * (0.5 + Math.random() * 0.5));
        return Duration.ofMillis(jittered);
    }

    @Override
    public BackoffStrategy withMaxDelay(Duration maxDelay) {
        return new JitteredBackoff(delegate.withMaxDelay(maxDelay));
    }

    @Override
    public BackoffStrategy withJitter() {
        return this;
    }
}
