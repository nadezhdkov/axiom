package io.axiom.concurrent.promise;

/**
 * State of a {@link Promise}. Starts {@code PENDING} and transitions to exactly one final state.
 */
public enum PromiseState {

    PENDING,
    FULFILLED,
    REJECTED,
    CANCELLED;

    public boolean isFinal() {
        return this != PENDING;
    }

    public boolean isSuccess() {
        return this == FULFILLED;
    }

    public boolean isFailure() {
        return this == REJECTED || this == CANCELLED;
    }
}
