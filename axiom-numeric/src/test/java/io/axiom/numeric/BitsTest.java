package io.axiom.numeric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitsTest {

    @Test
    void hasBitReadsIndividualBits() {
        I32 value = I32.of(0b1010);
        assertFalse(value.hasBit(0));
        assertTrue(value.hasBit(1));
        assertFalse(value.hasBit(2));
        assertTrue(value.hasBit(3));
    }

    @Test
    void hasBitRejectsOutOfRangeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> I8.of(0).hasBit(8));
        assertThrows(IndexOutOfBoundsException.class, () -> I8.of(0).hasBit(-1));
    }

    @Test
    void setClearToggleBit() {
        I32 value = I32.of(0);
        value = value.setBit(2);
        assertEquals(4, value.value());
        value = value.setBit(0);
        assertEquals(5, value.value());
        value = value.clearBit(2);
        assertEquals(1, value.value());
        value = value.toggleBit(0);
        assertEquals(0, value.value());
        value = value.toggleBit(3);
        assertEquals(8, value.value());
    }

    @Test
    void setBitOnMostSignificantBitOfSignedTypeFlipsSign() {
        // Bit 7 is the sign bit of an 8-bit signed value.
        I8 result = I8.of(0).setBit(7);
        assertEquals(-128, result.value());
    }

    @Test
    void countOnesLeadingTrailingZeros() {
        I32 value = I32.of(0b1010);
        assertEquals(2, value.countOnes());
        assertEquals(28, value.leadingZeros());
        assertEquals(1, value.trailingZeros());

        assertEquals(32, I32.of(0).trailingZeros());
        assertEquals(0, I8.of(0).countOnes());
        assertEquals(8, I8.of(-1).countOnes()); // -1 is all-ones in 8-bit two's complement
    }

    @Test
    void rotateLeftAndRight() {
        U8 value = U8.of(0b1000_0001);
        assertEquals(0b0000_0011, value.rotateLeft(1).value());
        assertEquals(0b1100_0000, value.rotateRight(1).value());
        assertEquals(value, value.rotateLeft(8)); // full rotation is a no-op
    }

    @Test
    void andOrXorNot() {
        U8 a = U8.of(0b1100);
        U8 b = U8.of(0b1010);
        assertEquals(U8.of(0b1000), a.and(b));
        assertEquals(U8.of(0b1110), a.or(b));
        assertEquals(U8.of(0b0110), a.xor(b));
        assertEquals(U8.of(0b1111_0011), a.not());
    }

    @Test
    void notOnSignedTypeMatchesTwosComplement() {
        assertEquals(I8.of(-6), I8.of(5).not());
    }

    @Test
    void composedBitOperations() {
        I32 result = I32.of(0)
            .setBit(0)
            .setBit(1)
            .setBit(2)
            .clearBit(1)
            .toggleBit(3);
        assertEquals(0b1101, result.value());
    }
}
