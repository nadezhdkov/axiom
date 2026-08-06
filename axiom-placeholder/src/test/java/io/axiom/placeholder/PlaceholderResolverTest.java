package io.axiom.placeholder;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderResolverTest {

    @Test
    void resolvesSimpleKey() {
        var resolver = PlaceholderResolver.of(Map.of("name", "Ada"));
        assertEquals("Hello, Ada!", resolver.resolve("Hello, ${name}!"));
    }

    @Test
    void resolvesMultiplePlaceholdersInOneTemplate() {
        var resolver = PlaceholderResolver.of(Map.of("host", "localhost", "port", "5432"));
        assertEquals("localhost:5432", resolver.resolve("${host}:${port}"));
    }

    @Test
    void usesDefaultWhenKeyMissing() {
        var resolver = PlaceholderResolver.of(Map.of());
        assertEquals("fallback", resolver.resolve("${missing:fallback}"));
    }

    @Test
    void emptyDefaultIsAllowed() {
        var resolver = PlaceholderResolver.of(Map.of());
        assertEquals("[]", resolver.resolve("[${missing:}]"));
    }

    @Test
    void throwsWhenKeyMissingAndNoDefault() {
        var resolver = PlaceholderResolver.of(Map.of());
        var ex = assertThrows(UnresolvedPlaceholderException.class, () -> resolver.resolve("${missing}"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void appliesBuiltinUpperTransform() {
        var resolver = PlaceholderResolver.of(Map.of("name", "ada"));
        assertEquals("ADA", resolver.resolve("${name|upper}"));
    }

    @Test
    void appliesTransformToDefaultValue() {
        var resolver = PlaceholderResolver.of(Map.of());
        assertEquals("FALLBACK", resolver.resolve("${missing:fallback|upper}"));
    }

    @Test
    void customTransformIsRegistrable() {
        var resolver = PlaceholderResolver.of(Map.<String, String>of("n", "5"))
                .withTransform("double", s -> String.valueOf(Integer.parseInt(s) * 2));
        assertEquals("10", resolver.resolve("${n|double}"));
    }

    @Test
    void unknownTransformThrows() {
        var resolver = PlaceholderResolver.of(Map.of("name", "ada"));
        assertThrows(PlaceholderException.class, () -> resolver.resolve("${name|unknown}"));
    }

    @Test
    void resolvedValueIsItselfRecursivelyResolved() {
        var resolver = PlaceholderResolver.of(Map.of("greeting", "Hello, ${name}", "name", "World"));
        assertEquals("Hello, World", resolver.resolve("${greeting}"));
    }

    @Test
    void plainTextWithoutPlaceholdersPassesThroughUnchanged() {
        var resolver = PlaceholderResolver.of(Map.of());
        assertEquals("no placeholders here", resolver.resolve("no placeholders here"));
    }

    @Test
    void unterminatedPlaceholderIsLeftAsLiteralTail() {
        var resolver = PlaceholderResolver.of(Map.of("x", "1"));
        assertEquals("prefix ${unterminated", resolver.resolve("prefix ${unterminated"));
    }

    // -------- Mandatory: circular reference detection (axiom.md §11) --------

    @Test
    void detectsDirectCircularReference() {
        var resolver = PlaceholderResolver.of(Map.of("a", "${b}", "b", "${a}"));
        var ex = assertThrows(CircularPlaceholderReferenceException.class, () -> resolver.resolve("${a}"));
        assertTrue(ex.getMessage().contains("a"));
        assertTrue(ex.getMessage().contains("b"));
    }

    @Test
    void detectsSelfReference() {
        var resolver = PlaceholderResolver.of(Map.of("a", "${a}"));
        assertThrows(CircularPlaceholderReferenceException.class, () -> resolver.resolve("${a}"));
    }

    @Test
    void detectsIndirectCircularReferenceThroughMultipleKeys() {
        var resolver = PlaceholderResolver.of(Map.of(
                "a", "${b}",
                "b", "${c}",
                "c", "${a}"
        ));
        assertThrows(CircularPlaceholderReferenceException.class, () -> resolver.resolve("${a}"));
    }

    @Test
    void doesNotFalselyFlagTheSameKeyResolvedTwiceInSeparateChains() {
        // "${a} and ${a}" resolves "a" twice, but never re-enters "a" *while already resolving it*
        var resolver = PlaceholderResolver.of(Map.of("a", "x"));
        assertEquals("x and x", resolver.resolve("${a} and ${a}"));
    }

    @Test
    void diamondReferenceWithoutACycleResolvesSuccessfully() {
        // a -> ${b} ${c}, b -> "1", c -> "2" — shared but non-cyclic, must not be flagged
        var resolver = PlaceholderResolver.of(Map.of("a", "${b} ${c}", "b", "1", "c", "2"));
        assertEquals("1 2", resolver.resolve("${a}"));
    }
}
