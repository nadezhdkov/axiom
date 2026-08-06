package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Unsigned 64-bit integer, range [0, 2^64-1]. Backed by the raw two's complement bit pattern of
 * a {@code long} — every {@code long} bit pattern is already a valid unsigned 64-bit value, so
 * {@link #of(long)} never throws, and unsigned arithmetic on the full range uses
 * {@link Long#compareUnsigned}-style reasoning under the hood via {@link #toBigInteger()}
 * ({@link Long#toUnsignedString(long)} for the negative-bit-pattern case). */
public final class U64 extends FixedWidth<U64> {

    public static final int BITS = 64;
    public static final int BYTES = 8;
    public static final U64 MIN = new U64(0L);
    public static final U64 MAX = new U64(-1L); // all 64 bits set = 2^64-1 unsigned

    private U64(long raw) {
        super(raw);
    }

    /** Every {@code long} bit pattern is a valid U64 — this never throws. */
    public static U64 of(long rawBits) {
        return new U64(rawBits);
    }

    /** The raw bit pattern; use {@link Long#toUnsignedString(long)} to render it as a value. */
    public long value() {
        return raw;
    }

    @Override
    protected int bits() {
        return BITS;
    }

    @Override
    protected boolean signed() {
        return false;
    }

    @Override
    protected String typeName() {
        return "U64";
    }

    @Override
    protected U64 create(long raw) {
        return new U64(raw);
    }

    public U64 add(U64 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public U64 subtract(U64 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public U64 multiply(U64 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public U64 addWrapping(U64 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public U64 subtractWrapping(U64 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public U64 multiplyWrapping(U64 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public U64 addSaturating(U64 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public U64 subtractSaturating(U64 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public U64 multiplySaturating(U64 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
