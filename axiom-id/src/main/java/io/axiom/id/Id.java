package io.axiom.id;

import io.axiom.id.internal.UlidGenerator;
import io.axiom.id.internal.UuidV7Generator;

import java.util.UUID;

/**
 * Static facade for generating time-sortable identifiers — the one thing {@link UUID} (v3/v4/v5
 * only) and the rest of the JDK have no built-in answer for.
 */
public final class Id {

    private Id() {
    }

    /** A new RFC 9562 UUIDv7: random-looking, but sortable by creation time. */
    public static UUID uuidV7() {
        return UuidV7Generator.next();
    }

    /** A new ULID: 26-character, lexicographically sortable by creation time. */
    public static Ulid ulid() {
        return Ulid.fromBytes(UlidGenerator.nextBytes());
    }
}
