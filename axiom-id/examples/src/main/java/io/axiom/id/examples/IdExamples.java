package io.axiom.id.examples;

import io.axiom.id.Id;
import io.axiom.id.Ulid;

import java.time.Instant;
import java.util.UUID;

public final class IdExamples {

    private IdExamples() {
    }

    public static void main(String[] args) {
        UUID orderId = Id.uuidV7();
        System.out.println("order id: " + orderId + " (version " + orderId.version() + ")");

        Ulid traceId = Id.ulid();
        System.out.println("trace id: " + traceId);

        Instant createdAt = traceId.timestamp();
        System.out.println("trace created at: " + createdAt);

        Ulid parsed = Ulid.of(traceId.toString());
        System.out.println("round-trip equal: " + traceId.equals(parsed));
    }
}
