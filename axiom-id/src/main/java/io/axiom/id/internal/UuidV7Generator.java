package io.axiom.id.internal;

import java.math.BigInteger;
import java.util.UUID;

/**
 * RFC 9562 UUIDv7 generation: 48-bit Unix ms timestamp, 4-bit version, 12-bit {@code rand_a},
 * 2-bit variant, 62-bit {@code rand_b} — {@code rand_a}/{@code rand_b} together form a 74-bit
 * monotonic sequence via {@link MonotonicClock} (its "Method 3" of the RFC).
 */
public final class UuidV7Generator {

    private static final int SEQUENCE_BITS = 74;
    private static final BigInteger RAND_A_MASK = BigInteger.valueOf(0xFFF);
    private static final long RAND_B_MASK = (1L << 62) - 1;

    private static final MonotonicClock CLOCK = new MonotonicClock(SEQUENCE_BITS);

    private UuidV7Generator() {
    }

    public static UUID next() {
        MonotonicClock.Tick tick = CLOCK.next();
        long timestamp48 = tick.millis() & 0xFFFFFFFFFFFFL;
        BigInteger sequence = tick.sequence();

        long randA = sequence.shiftRight(62).and(RAND_A_MASK).longValue();
        long randB = sequence.and(BigInteger.valueOf(RAND_B_MASK)).longValue();

        long mostSigBits = (timestamp48 << 16) | (0x7L << 12) | (randA & 0xFFF);
        long leastSigBits = (0b10L << 62) | (randB & RAND_B_MASK);

        return new UUID(mostSigBits, leastSigBits);
    }
}
