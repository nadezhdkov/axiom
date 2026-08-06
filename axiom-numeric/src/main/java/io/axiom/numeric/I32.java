package io.axiom.numeric;

import io.axiom.numeric.internal.FixedWidth;

/** Signed 32-bit integer — the same range as Java's {@code int}. */
public final class I32 extends FixedWidth<I32> {

    public static final int BITS = 32;
    public static final int BYTES = 4;
    public static final I32 MIN = new I32(Integer.MIN_VALUE);
    public static final I32 MAX = new I32(Integer.MAX_VALUE);

    private I32(long raw) {
        super(raw);
    }

    public static I32 of(int value) {
        return new I32(value);
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
        return "I32";
    }

    @Override
    protected I32 create(long raw) {
        return new I32(raw);
    }

    public I32 add(I32 other) {
        return checked(toBigInteger().add(other.toBigInteger()), "add");
    }

    public I32 subtract(I32 other) {
        return checked(toBigInteger().subtract(other.toBigInteger()), "subtract");
    }

    public I32 multiply(I32 other) {
        return checked(toBigInteger().multiply(other.toBigInteger()), "multiply");
    }

    public I32 addWrapping(I32 other) {
        return wrapping(toBigInteger().add(other.toBigInteger()));
    }

    public I32 subtractWrapping(I32 other) {
        return wrapping(toBigInteger().subtract(other.toBigInteger()));
    }

    public I32 multiplyWrapping(I32 other) {
        return wrapping(toBigInteger().multiply(other.toBigInteger()));
    }

    public I32 addSaturating(I32 other) {
        return saturating(toBigInteger().add(other.toBigInteger()));
    }

    public I32 subtractSaturating(I32 other) {
        return saturating(toBigInteger().subtract(other.toBigInteger()));
    }

    public I32 multiplySaturating(I32 other) {
        return saturating(toBigInteger().multiply(other.toBigInteger()));
    }

    // Named convenience conversions — the specific pair worked through in docs/axiom-numeric.md's
    // own "Conversões" example. Every other pair uses the generic convertChecked/convertWrapping/
    // convertSaturating(target) inherited from FixedWidth (see that class's Javadoc for why).

    public I8 toI8Checked() {
        return convertChecked(I8.MIN);
    }

    public I8 toI8Wrapping() {
        return convertWrapping(I8.MIN);
    }

    public I8 toI8Saturated() {
        return convertSaturating(I8.MIN);
    }
}
