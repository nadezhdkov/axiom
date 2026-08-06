package io.axiom.console.validate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorsTest {

    @Test
    void notBlankThrowsOnNullOrBlank() {
        assertThrows(ValidationException.class, () -> Validators.notBlank().validate(null));
        assertThrows(ValidationException.class, () -> Validators.notBlank().validate("   "));
        assertDoesNotThrow(() -> Validators.notBlank().validate("ok"));
    }

    @Test
    void rangeThrowsOutsideBoundsInclusive() {
        Validator<Integer> range = Validators.range(1, 10);
        assertDoesNotThrow(() -> range.validate(1));
        assertDoesNotThrow(() -> range.validate(10));
        assertThrows(ValidationException.class, () -> range.validate(0));
        assertThrows(ValidationException.class, () -> range.validate(11));
    }

    @Test
    void alwaysOkNeverThrows() {
        assertDoesNotThrow(() -> Validators.<String>alwaysOk().validate(null));
    }
}
