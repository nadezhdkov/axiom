package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Unsigned 32-bit integer, range [0, 4294967295]. Backed by {@code long} since {@code int} alone
 * cannot represent the full range (see {@code docs/axiom-numeric.md}'s own example: {@code
 * U32.of(4_000_000_000L)} already exceeds {@link Integer#MAX_VALUE}). */
public final class U32 extends FixedWidth<U32> {

    public static final int BITS = 32;
    public static final int BYTES = 4;
    public static final U32 MIN = new U32(0);
    public static final U32 MAX = new U32(4_294_967_295L);

    private U32(long raw) {
        super(raw);
    }

    /** @throws ArithmeticException if {@code value} does not fit in [0, 4294967295]. */
    public static U32 of(long value) {
        return MIN.checked(java.math.BigInteger.valueOf(value), "construct");
    }

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
        return "U32";
    }

    @Override
    protected U32 create(long raw) {
        return new U32(raw);
    }

    public U32 add(U32 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public U32 subtract(U32 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public U32 multiply(U32 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public U32 addWrapping(U32 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public U32 subtractWrapping(U32 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public U32 multiplyWrapping(U32 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public U32 addSaturating(U32 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public U32 subtractSaturating(U32 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public U32 multiplySaturating(U32 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
