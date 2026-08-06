package io.axiom.numeric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignedTypesTest {

    @Test
    void i8MinMaxZero() {
        assertEquals(-128, I8.MIN.value());
        assertEquals(127, I8.MAX.value());
        assertEquals(0, I8.of(0).value());
    }

    @Test
    void i8RejectsOutOfRangeConstruction() {
        assertThrows(ArithmeticException.class, () -> I8.of(128));
        assertThrows(ArithmeticException.class, () -> I8.of(-129));
    }

    @Test
    void i8CheckedAddThrowsOnOverflow() {
        assertThrows(ArithmeticException.class, () -> I8.MAX.add(I8.of(1)));
        assertThrows(ArithmeticException.class, () -> I8.MIN.subtract(I8.of(1)));
    }

    @Test
    void i8WrappingAddWrapsModularly() {
        assertEquals(I8.MIN, I8.MAX.addWrapping(I8.of(1)));
        assertEquals(I8.MAX, I8.MIN.subtractWrapping(I8.of(1)));
    }

    @Test
    void i8SaturatingAddClamps() {
        assertEquals(I8.MAX, I8.MAX.addSaturating(I8.of(1)));
        assertEquals(I8.MIN, I8.MIN.subtractSaturating(I8.of(1)));
    }

    @Test
    void i8MultiplyOverflowAcrossAllThreeSemantics() {
        I8 hundred = I8.of(100);
        assertThrows(ArithmeticException.class, () -> hundred.multiply(hundred));
        assertEquals(I8.of(16), hundred.multiplyWrapping(hundred)); // 10000 mod 256, sign-normalized
        assertEquals(I8.MAX, hundred.multiplySaturating(hundred));
    }

    @Test
    void i16BoundaryOverflow() {
        assertThrows(ArithmeticException.class, () -> I16.MAX.add(I16.of(1)));
        assertEquals(I16.MIN, I16.MAX.addWrapping(I16.of(1)));
        assertEquals(I16.MAX, I16.MAX.addSaturating(I16.of(1)));
    }

    @Test
    void i32BoundaryOverflow() {
        assertEquals(Integer.MIN_VALUE, I32.MIN.value());
        assertEquals(Integer.MAX_VALUE, I32.MAX.value());
        assertThrows(ArithmeticException.class, () -> I32.MAX.add(I32.of(1)));
        assertEquals(I32.MIN, I32.MAX.addWrapping(I32.of(1)));
        assertEquals(I32.MAX, I32.MAX.addSaturating(I32.of(1)));
    }

    @Test
    void i64BoundaryOverflow() {
        assertEquals(Long.MIN_VALUE, I64.MIN.value());
        assertEquals(Long.MAX_VALUE, I64.MAX.value());
        assertThrows(ArithmeticException.class, () -> I64.MAX.add(I64.of(1)));
        assertEquals(I64.MIN, I64.MAX.addWrapping(I64.of(1)));
        assertEquals(I64.MAX, I64.MAX.addSaturating(I64.of(1)));
    }

    @Test
    void chainedCompositeArithmetic() {
        I32 result = I32.of(10).add(I32.of(5)).multiply(I32.of(2)).subtract(I32.of(3));
        assertEquals(27, result.value());
    }
}
