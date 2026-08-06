package io.axiom.id;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdUuidV7Test {

    @Test
    void versionNibbleIsSeven() {
        UUID id = Id.uuidV7();
        assertEquals(7, id.version());
    }

    @Test
    void variantIsIetf() {
        UUID id = Id.uuidV7();
        assertEquals(2, id.variant());
    }

    @Test
    void embeddedTimestampIsPlausible() {
        long before = System.currentTimeMillis();
        UUID id = Id.uuidV7();
        long after = System.currentTimeMillis();

        long timestamp = id.getMostSignificantBits() >>> 16;
        assertTrue(timestamp >= before && timestamp <= after,
            "embedded timestamp " + Instant.ofEpochMilli(timestamp) + " outside generation window");
    }

    @Test
    void backToBackCallsAreMonotonicallySortable() {
        UUID[] ids = new UUID[1000];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = Id.uuidV7();
        }
        for (int i = 1; i < ids.length; i++) {
            assertTrue(ids[i - 1].compareTo(ids[i]) < 0,
                "expected " + ids[i - 1] + " < " + ids[i]);
        }
    }

    @Test
    void generatesUniqueValuesAtVolume() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertTrue(seen.add(Id.uuidV7()), "duplicate UUIDv7 generated");
        }
    }

    @Test
    void roundTripsThroughStringForm() {
        UUID id = Id.uuidV7();
        assertEquals(id, UUID.fromString(id.toString()));
    }
}
