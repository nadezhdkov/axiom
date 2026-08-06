package io.axiom.numeric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnsignedTypesTest {

    @Test
    void u8MinMaxZero() {
        assertEquals(0, U8.MIN.value());
        assertEquals(255, U8.MAX.value());
        assertEquals(0, U8.of(0).value());
    }

    @Test
    void u8RejectsNegativeAndOverflow() {
        assertThrows(ArithmeticException.class, () -> U8.of(-1));
        assertThrows(ArithmeticException.class, () -> U8.of(256));
    }

    @Test
    void u8CheckedSubtractThrowsBelowZero() {
        assertThrows(ArithmeticException.class, () -> U8.MIN.subtract(U8.of(1)));
    }

    @Test
    void u8WrappingSubtractWrapsToMax() {
        assertEquals(U8.MAX, U8.MIN.subtractWrapping(U8.of(1)));
    }

    @Test
    void u8SaturatingSubtractClampsToZero() {
        assertEquals(U8.MIN, U8.MIN.subtractSaturating(U8.of(1)));
    }

    @Test
    void u16Boundary() {
        assertEquals(65535, U16.MAX.value());
        assertThrows(ArithmeticException.class, () -> U16.MAX.add(U16.of(1)));
        assertEquals(U16.MIN, U16.MAX.addWrapping(U16.of(1)));
    }

    @Test
    void u32SupportsValueBeyondIntRange() {
        // The exact example from docs/axiom-numeric.md's "Exemplo Rápido".
        U32 packetLength = U32.of(4_000_000_000L);
        assertEquals(4_000_000_000L, packetLength.value());
        assertEquals(4_294_967_295L, U32.MAX.value());
    }

    @Test
    void u32RejectsOutOfRange() {
        assertThrows(ArithmeticException.class, () -> U32.of(-1L));
        assertThrows(ArithmeticException.class, () -> U32.of(4_294_967_296L));
    }

    @Test
    void u64NeverThrowsOnConstruction() {
        // Every long bit pattern, including "negative" ones, is a valid unsigned 64-bit value.
        assertEquals(-1L, U64.MAX.value());
        assertEquals(Long.toUnsignedString(-1L), U64.MAX.toBigInteger().toString());
    }

    @Test
    void u64CheckedAddThrowsAtTop() {
        assertThrows(ArithmeticException.class, () -> U64.MAX.add(U64.of(1)));
    }

    @Test
    void u64WrappingAddWrapsToZero() {
        assertEquals(U64.MIN, U64.MAX.addWrapping(U64.of(1)));
    }

    @Test
    void u64SaturatingAddClampsToMax() {
        assertEquals(U64.MAX, U64.MAX.addSaturating(U64.of(1)));
    }
}
