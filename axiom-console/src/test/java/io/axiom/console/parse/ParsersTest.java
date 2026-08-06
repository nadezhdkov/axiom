package io.axiom.console.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsersTest {

    @Test
    void i32ParsesTrimmedInput() {
        assertEquals(42, Parsers.i32().parse("  42  "));
    }

    @Test
    void i32ThrowsOnNonNumericInput() {
        assertThrows(ParseFailureException.class, () -> Parsers.i32().parse("abc"));
    }

    @Test
    void boolRecognizesTruthyAndFalsyTokensCaseInsensitively() {
        assertTrue(Parsers.bool().parse("YES"));
        assertTrue(Parsers.bool().parse("sim"));
        assertFalse(Parsers.bool().parse("n"));
        assertFalse(Parsers.bool().parse("0"));
    }

    @Test
    void boolThrowsOnUnrecognizedToken() {
        assertThrows(ParseFailureException.class, () -> Parsers.bool().parse("maybe"));
    }

    @Test
    void chRequiresExactlyOneCharacterAfterTrimming() {
        assertEquals('x', Parsers.ch().parse("  x  "));
        assertThrows(ParseFailureException.class, () -> Parsers.ch().parse("xy"));
    }

    @Test
    void stringDoesNotTrimTheRawInput() {
        assertEquals("  raw  ", Parsers.string().parse("  raw  "));
    }
}
