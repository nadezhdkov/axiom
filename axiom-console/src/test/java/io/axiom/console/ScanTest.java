package io.axiom.console;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanTest {

    @AfterEach
    void resetDefaultEngine() {
        Scan.use(InputScanner.console());
    }

    @Test
    void useSwapsTheDefaultEngine() {
        Scan.use(InputScanner.fromString("hello\n"));
        assertEquals("hello", Scan.line());
    }

    @Test
    void i32DelegatesToTheSwappedEngine() {
        Scan.use(InputScanner.fromString("99\n"));
        assertEquals(99, Scan.i32("n"));
    }

    @Test
    void useIgnoresANullEngine() {
        Scan.use(InputScanner.fromString("kept\n"));
        Scan.use(null);
        assertEquals("kept", Scan.line());
    }
}
