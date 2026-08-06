package io.axiom.id.internal;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free millisecond clock producing a monotonically increasing sequence when two ticks land
 * in the same millisecond, instead of two independent random draws — this is what keeps
 * back-to-back {@link io.axiom.id.Id#uuidV7()}/{@link io.axiom.id.Id#ulid()} calls sortable.
 * State is held behind a single {@link AtomicReference}, never an unsynchronized mutable field
 * (see {@code CLAUDE.md} on static mutable state), and never depends on {@code axiom-concurrent} —
 * a plain {@code AtomicReference} already covers the need.
 */
public final class MonotonicClock {

    /** One tick: the millisecond it belongs to, and the sequence value within that millisecond. */
    public record Tick(long millis, BigInteger sequence) {
    }

    private final int sequenceBits;
    private final BigInteger sequenceMask;
    private final SecureRandom random = new SecureRandom();
    private final AtomicReference<Tick> state = new AtomicReference<>(new Tick(0L, BigInteger.ZERO));

    public MonotonicClock(int sequenceBits) {
        this.sequenceBits = sequenceBits;
        this.sequenceMask = BigInteger.ONE.shiftLeft(sequenceBits).subtract(BigInteger.ONE);
    }

    public Tick next() {
        Tick prev;
        Tick next;
        do {
            prev = state.get();
            long now = System.currentTimeMillis();
            if (now > prev.millis()) {
                next = new Tick(now, randomSequence());
            } else {
                BigInteger incremented = prev.sequence().add(BigInteger.ONE);
                if (incremented.bitLength() > sequenceBits) {
                    // Exhausted the sequence space within this millisecond (would need 2^sequenceBits
                    // ids in 1ms) — borrow the next millisecond instead of wrapping around.
                    next = new Tick(prev.millis() + 1, BigInteger.ZERO);
                } else {
                    next = new Tick(prev.millis(), incremented);
                }
            }
        } while (!state.compareAndSet(prev, next));
        return next;
    }

    private BigInteger randomSequence() {
        byte[] bytes = new byte[(sequenceBits + 7) / 8];
        random.nextBytes(bytes);
        return new BigInteger(1, bytes).and(sequenceMask);
    }

    /** Renders a non-negative value as a fixed-length big-endian byte array, zero-padded. */
    public static byte[] toFixedBytes(BigInteger value, int length) {
        byte[] raw = value.toByteArray();
        byte[] out = new byte[length];
        int copyLen = Math.min(raw.length, length);
        System.arraycopy(raw, raw.length - copyLen, out, length - copyLen, copyLen);
        return out;
    }
}
