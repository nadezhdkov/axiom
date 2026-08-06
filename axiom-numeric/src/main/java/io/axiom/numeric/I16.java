package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Signed 16-bit integer, range [-32768, 32767]. */
public final class I16 extends FixedWidth<I16> {

    public static final int BITS = 16;
    public static final int BYTES = 2;
    public static final I16 MIN = new I16(-32768);
    public static final I16 MAX = new I16(32767);

    private I16(long raw) {
        super(raw);
    }

    /** @throws ArithmeticException if {@code value} does not fit in [-32768, 32767]. */
    public static I16 of(int value) {
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
        return "I16";
    }

    @Override
    protected I16 create(long raw) {
        return new I16(raw);
    }

    public I16 add(I16 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public I16 subtract(I16 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public I16 multiply(I16 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public I16 addWrapping(I16 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public I16 subtractWrapping(I16 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public I16 multiplyWrapping(I16 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public I16 addSaturating(I16 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public I16 subtractSaturating(I16 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public I16 multiplySaturating(I16 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }
}
