package io.axiom.dotenv.internal;

import io.axiom.dotenv.DotenvException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DotenvTypeConverterTest {

    /** A type with no registered converter, not an enum, not a List/Set. */
    private record Unsupported(String value) {
    }

    @Test
    void unsupportedTypeThrowsDotenvExceptionNotIllegalArgumentException() {
        // Regression: this used to throw a raw IllegalArgumentException, breaking the module's
        // exception hierarchy (everything else under DotenvException) for callers inspecting
        // DotenvInjectionException#getCause().
        assertThrows(DotenvException.class, () -> DotenvTypeConverter.convert("x", Unsupported.class));
    }
}
