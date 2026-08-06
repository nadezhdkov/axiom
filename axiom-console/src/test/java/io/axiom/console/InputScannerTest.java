package io.axiom.console;

import io.axiom.console.error.ErrorCode;
import io.axiom.console.error.ScanError;
import io.axiom.console.parse.ParseFailureException;
import io.axiom.console.parse.Parsers;
import io.axiom.console.validate.Validators;
import io.axiom.core.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputScannerTest {

    @Test
    void readsSequentialLinesFromAStringSource() {
        InputHandler handler = InputScanner.fromString("10\n20\n30\n");

        assertEquals(10, handler.read(Parsers.i32()));
        assertEquals(20, handler.read(Parsers.i32()));
        assertEquals(30, handler.read(Parsers.i32()));
    }

    @Test
    void hasNextLineReflectsRemainingInputWithoutConsumingIt() {
        InputHandler handler = InputScanner.fromString("only-line");

        assertTrue(handler.hasNextLine());
        assertEquals("only-line", handler.line());
        assertFalse(handler.hasNextLine());
    }

    @Test
    void readThrowsParseFailureExceptionOnInvalidInput() {
        InputHandler handler = InputScanner.fromString("not-a-number\n");
        assertThrows(ParseFailureException.class, () -> handler.read(Parsers.i32()));
    }

    @Test
    void untilRepromptsUntilAValidValueIsRead() {
        InputHandler handler = InputScanner.fromString("abc\n-5\n42\n");
        int value = handler.until("n", Parsers.i32(), Validators.range(0, 100));
        assertEquals(42, value);
    }

    @Test
    void tryReadReturnsOkOnValidInput() {
        InputHandler handler = InputScanner.fromString("7\n");
        Result<Integer, ScanError> result = handler.tryRead(Parsers.i32());
        assertTrue(result.isOk());
        assertEquals(7, result.orNull());
    }

    @Test
    void tryReadReturnsParseErrorOnInvalidInputWithoutThrowing() {
        InputHandler handler = InputScanner.fromString("not-a-number\n");
        Result<Integer, ScanError> result = handler.tryRead(Parsers.i32());
        assertTrue(result.isErr());
        assertEquals(ErrorCode.PARSE_ERROR, result.errorOrThrow().code());
    }

    @Test
    void tryReadReturnsValidationErrorWhenValidatorRejectsTheValue() {
        InputHandler handler = InputScanner.fromString("500\n");
        Result<Integer, ScanError> result = handler.tryRead("n", Parsers.i32(), Validators.range(0, 100));
        assertTrue(result.isErr());
        assertEquals(ErrorCode.VALIDATION_ERROR, result.errorOrThrow().code());
    }

    @Test
    void tryReadReturnsEofErrorWhenNoMoreInputIsAvailable() {
        InputHandler handler = InputScanner.fromString("");
        Result<Integer, ScanError> result = handler.tryRead(Parsers.i32());
        assertTrue(result.isErr());
        assertEquals(ErrorCode.EOF, result.errorOrThrow().code());
    }

    @Test
    void nullContentIsTreatedAsEmptyInput() {
        InputHandler handler = InputScanner.fromString(null);
        assertFalse(handler.hasNextLine());
    }
}
