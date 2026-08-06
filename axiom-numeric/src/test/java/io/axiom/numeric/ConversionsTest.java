package io.axiom.numeric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionsTest {

    @Test
    void namedI32ToI8Conversions() {
        // The exact example from docs/axiom-numeric.md's "Conversões" section.
        I32 value = I32.of(300);

        assertThrows(ArithmeticException.class, value::toI8Checked);
        assertEquals(I8.of(300 - 256), value.toI8Wrapping());
        assertEquals(I8.MAX, value.toI8Saturated());
    }

    @Test
    void widenSafeConversionNeverThrows() {
        I32 small = I32.of(42);
        assertEquals(I8.of(42), small.convertChecked(I8.MIN));
    }

    @Test
    void genericConversionWorksBetweenArbitraryTypes() {
        U16 value = U16.of(40000); // within U16 range, out of I8 range
        assertThrows(ArithmeticException.class, () -> value.convertChecked(I8.MIN));
        // Wrapping into an 8-bit target only keeps the low 8 bits: 40000 mod 256 = 64.
        assertEquals(I8.of(64), value.convertWrapping(I8.MIN));
    }

    @Test
    void saturatingConversionAcrossSignedness() {
        I32 negative = I32.of(-5);
        assertEquals(U8.MIN, negative.convertSaturating(U8.MIN));
    }
}
