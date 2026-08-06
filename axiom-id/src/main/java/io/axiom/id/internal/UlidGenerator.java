package io.axiom.id.internal;

import java.nio.ByteBuffer;

/**
 * ULID generation: 48-bit Unix ms timestamp followed by an 80-bit monotonic sequence
 * (see {@link MonotonicClock}), rendered as 16 raw bytes for {@link Crockford#encode(byte[])}.
 */
public final class UlidGenerator {

    private static final int SEQUENCE_BITS = 80;
    private static final MonotonicClock CLOCK = new MonotonicClock(SEQUENCE_BITS);

    private UlidGenerator() {
    }

    public static byte[] nextBytes() {
        MonotonicClock.Tick tick = CLOCK.next();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        long timestamp48 = tick.millis() & 0xFFFFFFFFFFFFL;
        for (int i = 5; i >= 0; i--) {
            buffer.put((byte) (timestamp48 >>> (i * 8)));
        }
        byte[] sequenceBytes = MonotonicClock.toFixedBytes(tick.sequence(), 10);
        buffer.put(sequenceBytes);
        return buffer.array();
    }

    /** Decodes the 48-bit timestamp embedded in the first 6 bytes of a ULID's raw form. */
    public static long timestampMillis(byte[] bytes16) {
        long millis = 0;
        for (int i = 0; i < 6; i++) {
            millis = (millis << 8) | (bytes16[i] & 0xFFL);
        }
        return millis;
    }
}
