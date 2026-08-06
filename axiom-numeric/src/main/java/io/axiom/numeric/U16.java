package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Unsigned 16-bit integer, range [0, 65535]. */
public final class U16 extends FixedWidth<U16> {

    public static final int BITS = 16;
    public static final int BYTES = 2;
    public static final U16 MIN = new U16(0);
    public static final U16 MAX = new U16(65535);

    private U16(long raw) {
        super(raw);
    }

    /** @throws ArithmeticException if {@code value} does not fit in [0, 65535]. */
    public static U16 of(int value) {
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
        return "U16";
    }

    @Override
    protected U16 create(long raw) {
        return new U16(raw);
    }

    public U16 add(U16 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public U16 subtract(U16 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public U16 multiply(U16 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public U16 addWrapping(U16 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public U16 subtractWrapping(U16 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public U16 multiplyWrapping(U16 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public U16 addSaturating(U16 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public U16 subtractSaturating(U16 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public U16 multiplySaturating(U16 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
