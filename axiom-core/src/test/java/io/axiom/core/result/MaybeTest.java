package io.axiom.core.result;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaybeTest {

    @Test
    void someHoldsValue() {
        Maybe<String> m = Maybe.some("hello");
        assertTrue(m.isPresent());
        assertEquals("hello", m.get());
    }

    @Test
    void noneIsEmpty() {
        Maybe<String> m = Maybe.none();
        assertTrue(m.isEmpty());
    }

    @Test
    void ofNullBecomesNone() {
        assertTrue(Maybe.of(null).isEmpty());
        assertTrue(Maybe.of("x").isPresent());
    }

    @Test
    void mapTransformsPresentValueAndPreservesAbsence() {
        assertEquals(5, Maybe.some("hello").map(String::length).get());
        assertTrue(Maybe.<String>none().map(String::length).isEmpty());
    }

    @Test
    void flatMapChains() {
        Maybe<Integer> result = Maybe.some("10").flatMap(s -> Maybe.some(Integer.parseInt(s)));
        assertEquals(10, result.get());
    }

    @Test
    void filterConvertsNonMatchingToNone() {
        Maybe<Integer> even = Maybe.some(10).filter(n -> n % 2 == 0);
        Maybe<Integer> odd = Maybe.some(11).filter(n -> n % 2 == 0);

        assertTrue(even.isPresent());
        assertTrue(odd.isEmpty());
    }

    @Test
    void orElseReturnsFallbackWhenAbsent() {
        assertEquals("default", Maybe.<String>none().orElse("default"));
        assertEquals("value", Maybe.some("value").orElse("default"));
    }

    @Test
    void orElseThrowThrowsForNone() {
        assertThrows(NoSuchElementException.class, () -> Maybe.<String>none().orElseThrow());
    }

    @Test
    void noneWithReasonIsPreservedInMessage() {
        NoSuchElementException ex = assertThrows(
                NoSuchElementException.class,
                () -> Maybe.<String>none("not found in cache").orElseThrow()
        );
        assertTrue(ex.getMessage().contains("not found in cache"));
    }

    @Test
    void toOptionalReflectsSomeAndNone() {
        assertTrue(Maybe.some("x").toOptional().isPresent());
        assertFalse(Maybe.none().toOptional().isPresent());
    }
}
