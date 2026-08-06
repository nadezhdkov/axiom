package io.axiom.numeric.internal;

import java.math.BigInteger;

/**
 * Shared checked/wrapping/saturating arithmetic, bit operations, and cross-type conversion for
 * every fixed-width type in this module (I8..I64, U8..U64). Not exported — each concrete public
 * type wraps this with its own accessor type ({@code int} vs {@code long}) and static
 * {@code MIN}/{@code MAX}/{@code BITS}/{@code BYTES} constants. Kept in {@code internal} even
 * though several methods here are {@code public}: what is hidden from consumers is this
 * <em>class</em> (never exported in {@code module-info.java}), not the members it contributes to
 * the public subclasses — same idiom already used for the third-party engine wrappers in
 * {@code axiom-json}/{@code axiom-yaml}, applied here to shared logic instead.
 *
 * <p>All arithmetic here goes through {@link BigInteger} rather than hand-rolled bit-overflow
 * detection. That costs some performance (see the module README's "Notas de Design" for an
 * honest accounting), but it is the same trade already made once in this project — favoring an
 * obviously-correct implementation over a clever one that is easy to get subtly wrong.
 *
 * @param <T> the concrete fixed-width type extending this class (CRTP — lets {@link #create}
 *     return the correct concrete type from generic code)
 */
public abstract class FixedWidth<T extends FixedWidth<T>> {

    /** The two's complement bit pattern of this value, in a 64-bit container regardless of width. */
    protected final long raw;

    protected FixedWidth(long raw) {
        this.raw = raw;
    }

    protected abstract int bits();

    protected abstract boolean signed();

    protected abstract String typeName();

    protected abstract T create(long raw);

    public final BigInteger toBigInteger() {
        if (!signed()) {
            return raw >= 0 ? BigInteger.valueOf(raw) : new BigInteger(Long.toUnsignedString(raw));
        }
        return BigInteger.valueOf(raw);
    }

    public final BigInteger min() {
        return signed() ? BigInteger.ONE.shiftLeft(bits() - 1).negate() : BigInteger.ZERO;
    }

    public final BigInteger max() {
        return signed()
            ? BigInteger.ONE.shiftLeft(bits() - 1).subtract(BigInteger.ONE)
            : BigInteger.ONE.shiftLeft(bits()).subtract(BigInteger.ONE);
    }

    // ── checked / wrapping / saturating construction from an arbitrary BigInteger ──────────────
    // Used both for same-type arithmetic (this.checked(a+b, "add")) and, via a target-type
    // prototype instance such as I8.MIN, for cross-type conversion (see FixedWidth#convertChecked
    // and friends) — bits()/signed()/create() only ever depend on the concrete type, never on
    // `this.raw`, so any existing instance of the target type works as the "descriptor".

    public final T checked(BigInteger candidate, String operation) {
        if (candidate.compareTo(min()) < 0 || candidate.compareTo(max()) > 0) {
            throw new ArithmeticException(typeName() + " overflow on " + operation + ": result "
                + candidate + " outside [" + min() + ", " + max() + "]");
        }
        return create(candidate.longValue());
    }

    public final T wrapping(BigInteger candidate) {
        BigInteger modulus = BigInteger.ONE.shiftLeft(bits());
        BigInteger unsignedBits = candidate.mod(modulus);
        if (signed()) {
            BigInteger half = BigInteger.ONE.shiftLeft(bits() - 1);
            if (unsignedBits.compareTo(half) >= 0) {
                unsignedBits = unsignedBits.subtract(modulus);
            }
        }
        return create(unsignedBits.longValue());
    }

    public final T saturating(BigInteger candidate) {
        if (candidate.compareTo(min()) < 0) {
            return create(min().longValue());
        }
        if (candidate.compareTo(max()) > 0) {
            return create(max().longValue());
        }
        return create(candidate.longValue());
    }

    // ── cross-type conversion ───────────────────────────────────────────────────────────────────
    // Generic on purpose rather than one named toI8Checked()/toU16Wrapping()/... per pair (8x8
    // combinations): a small, orthogonal API beats dozens of near-identical overloads, per
    // architecture.md §7.4 ("API pequena e composável"). `target` is any existing instance of the
    // desired type (its own value is irrelevant, only its bits()/signed()/create() matter) —
    // typically the type's own MIN or MAX constant, e.g. `value.convertChecked(I8.MIN)`.

    public final <R extends FixedWidth<R>> R convertChecked(R target) {
        return target.checked(toBigInteger(), "convert " + typeName() + " to " + target.typeName());
    }

    public final <R extends FixedWidth<R>> R convertWrapping(R target) {
        return target.wrapping(toBigInteger());
    }

    public final <R extends FixedWidth<R>> R convertSaturating(R target) {
        return target.saturating(toBigInteger());
    }

    // ── bit operations ──────────────────────────────────────────────────────────────────────────

    private long mask() {
        return bits() == 64 ? -1L : (1L << bits()) - 1;
    }

    private long normalize(long value) {
        long maskedValue = value & mask();
        if (!signed()) {
            return maskedValue;
        }
        long signBit = 1L << (bits() - 1);
        return (maskedValue & signBit) != 0 ? maskedValue - (mask() + 1) : maskedValue;
    }

    private void checkBitIndex(int index) {
        if (index < 0 || index >= bits()) {
            throw new IndexOutOfBoundsException(
                "bit index " + index + " out of range for " + bits() + "-bit " + typeName());
        }
    }

    public final boolean hasBit(int index) {
        checkBitIndex(index);
        return ((raw >>> index) & 1L) != 0;
    }

    public final T setBit(int index) {
        checkBitIndex(index);
        return create(normalize(raw | (1L << index)));
    }

    public final T clearBit(int index) {
        checkBitIndex(index);
        return create(normalize(raw & ~(1L << index)));
    }

    public final T toggleBit(int index) {
        checkBitIndex(index);
        return create(normalize(raw ^ (1L << index)));
    }

    public final int countOnes() {
        return Long.bitCount(raw & mask());
    }

    public final int leadingZeros() {
        return Long.numberOfLeadingZeros(raw & mask()) - (64 - bits());
    }

    public final int trailingZeros() {
        long masked = raw & mask();
        return masked == 0 ? bits() : Long.numberOfTrailingZeros(masked);
    }

    public final T rotateLeft(int distance) {
        int d = Math.floorMod(distance, bits());
        long masked = raw & mask();
        long rotated = ((masked << d) | (masked >>> (bits() - d))) & mask();
        return create(normalize(rotated));
    }

    public final T rotateRight(int distance) {
        return rotateLeft(bits() - Math.floorMod(distance, bits()));
    }

    public final T and(T other) {
        return create(normalize(raw & other.raw));
    }

    public final T or(T other) {
        return create(normalize(raw | other.raw));
    }

    public final T xor(T other) {
        return create(normalize(raw ^ other.raw));
    }

    public final T not() {
        return create(normalize(~raw));
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof FixedWidth<?> other
            && getClass() == other.getClass()
            && raw == other.raw;
    }

    @Override
    public final int hashCode() {
        return Long.hashCode(raw) * 31 + getClass().hashCode();
    }

    @Override
    public final String toString() {
        return typeName() + "(" + toBigInteger() + ")";
    }
}
