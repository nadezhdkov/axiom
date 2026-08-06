package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Unsigned 8-bit integer, range [0, 255]. */
public final class U8 extends FixedWidth<U8> {

    public static final int BITS = 8;
    public static final int BYTES = 1;
    public static final U8 MIN = new U8(0);
    public static final U8 MAX = new U8(255);

    private U8(long raw) {
        super(raw);
    }

    /** @throws ArithmeticException if {@code value} does not fit in [0, 255]. */
    public static U8 of(int value) {
        return MIN.checked(java.math.BigInteger.valueOf(value), "construct");
    }

    public int value() {
        return (int) raw;
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
        return "U8";
    }

    @Override
    protected U8 create(long raw) {
        return new U8(raw);
    }

    public U8 add(U8 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public U8 subtract(U8 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public U8 multiply(U8 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public U8 addWrapping(U8 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public U8 subtractWrapping(U8 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public U8 multiplyWrapping(U8 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public U8 addSaturating(U8 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public U8 subtractSaturating(U8 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public U8 multiplySaturating(U8 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
