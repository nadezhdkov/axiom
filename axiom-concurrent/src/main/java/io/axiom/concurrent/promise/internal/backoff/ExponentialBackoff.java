package io.axiom.concurrent.promise.internal.backoff;

import io.axiom.concurrent.promise.BackoffStrategy;

import java.time.Duration;

public class ExponentialBackoff implements BackoffStrategy {

    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;
    private final boolean useJitter;

    public ExponentialBackoff(Duration initialDelay, double multiplier, Duration maxDelay, boolean useJitter) {
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
        this.useJitter = useJitter;
    }

    @Override
    public Duration calculateDelay(int attempt) {
        long millis = initialDelay.toMillis();
        long delay = (long) (millis * Math.pow(multiplier, attempt - 1));

        if (maxDelay != null && delay > maxDelay.toMillis()) {
            delay = maxDelay.toMillis();
        }
        if (useJitter) {
            delay = (long) (delay * (0.5 + Math.random() * 0.5));
        }
        return Duration.ofMillis(delay);
    }

    @Override
    public BackoffStrategy withMaxDelay(Duration maxDelay) {
        return new ExponentialBackoff(initialDelay, multiplier, maxDelay, useJitter);
    }

    @Override
    public BackoffStrategy withJitter() {
        return new ExponentialBackoff(initialDelay, multiplier, maxDelay, true);
    }
}
