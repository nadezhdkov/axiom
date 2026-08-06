package io.axiom.concurrent.promise.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates multiple {@link Throwable}s from concurrent promise operations
 * ({@link io.axiom.concurrent.promise.combinators.PromiseAll#aggregateResults},
 * {@link io.axiom.concurrent.promise.combinators.PromiseAny}) into a single exception.
 */
public class AggregateException extends PromiseException {

    private final List<Throwable> errors;

    public AggregateException(List<Throwable> errors) {
        super(buildMessage(errors));
        this.errors = new ArrayList<>(errors);
    }

    public AggregateException(String message, List<Throwable> errors) {
        super(message);
        this.errors = new ArrayList<>(errors);
    }

    public List<Throwable> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public int getErrorCount() {
        return errors.size();
    }

    public Throwable getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    private static String buildMessage(List<Throwable> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Multiple errors occurred";
        }
        if (errors.size() == 1) {
            return "1 error occurred: " + errors.get(0).getMessage();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(errors.size()).append(" errors occurred:");
        for (int i = 0; i < Math.min(errors.size(), 3); i++) {
            sb.append("\n  ").append(i + 1).append(". ").append(errors.get(i).getClass().getSimpleName());
            String msg = errors.get(i).getMessage();
            if (msg != null && !msg.isEmpty()) sb.append(": ").append(msg);
        }
        if (errors.size() > 3) {
            sb.append("\n  ... and ").append(errors.size() - 3).append(" more");
        }
        return sb.toString();
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        System.err.println("Aggregated errors:");
        for (int i = 0; i < errors.size(); i++) {
            System.err.println("  [" + (i + 1) + "]");
            errors.get(i).printStackTrace();
        }
    }
}
