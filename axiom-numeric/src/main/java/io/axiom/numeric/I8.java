package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Signed 8-bit integer, range [-128, 127]. */
public final class I8 extends FixedWidth<I8> {

    public static final int BITS = 8;
    public static final int BYTES = 1;
    public static final I8 MIN = new I8(-128);
    public static final I8 MAX = new I8(127);

    private I8(long raw) {
        super(raw);
    }

    /** @throws ArithmeticException if {@code value} does not fit in [-128, 127]. */
    public static I8 of(int value) {
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
        return true;
    }

    @Override
    protected String typeName() {
        return "I8";
    }

    @Override
    protected I8 create(long raw) {
        return new I8(raw);
    }

    public I8 add(I8 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public I8 subtract(I8 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public I8 multiply(I8 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public I8 addWrapping(I8 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public I8 subtractWrapping(I8 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public I8 multiplyWrapping(I8 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public I8 addSaturating(I8 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public I8 subtractSaturating(I8 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public I8 multiplySaturating(I8 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
