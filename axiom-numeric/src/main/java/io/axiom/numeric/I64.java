package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Signed 64-bit integer — the same range as Java's {@code long}. */
public final class I64 extends FixedWidth<I64> {

    public static final int BITS = 64;
    public static final int BYTES = 8;
    public static final I64 MIN = new I64(Long.MIN_VALUE);
    public static final I64 MAX = new I64(Long.MAX_VALUE);

    private I64(long raw) {
        super(raw);
    }

    public static I64 of(long value) {
        return new I64(value);
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
        return true;
    }

    @Override
    protected String typeName() {
        return "I64";
    }

    @Override
    protected I64 create(long raw) {
        return new I64(raw);
    }

    public I64 add(I64 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public I64 subtract(I64 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public I64 multiply(I64 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public I64 addWrapping(I64 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public I64 subtractWrapping(I64 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public I64 multiplyWrapping(I64 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public I64 addSaturating(I64 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public I64 subtractSaturating(I64 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public I64 multiplySaturating(I64 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
