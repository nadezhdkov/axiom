package io.axiom.placeholder;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderSourceTest {

    @Test
    void ofMapResolvesPresentKeys() {
        PlaceholderSource source = PlaceholderSource.of(Map.of("k", "v"));
        assertEquals(Optional.of("v"), source.resolve("k"));
        assertTrue(source.resolve("missing").isEmpty());
    }

    @Test
    void orElseFallsBackToSecondSource() {
        PlaceholderSource primary = PlaceholderSource.of(Map.of("a", "1"));
        PlaceholderSource fallback = PlaceholderSource.of(Map.of("a", "ignored", "b", "2"));

        PlaceholderSource combined = primary.orElse(fallback);

        assertEquals(Optional.of("1"), combined.resolve("a"));
        assertEquals(Optional.of("2"), combined.resolve("b"));
        assertFalse(combined.resolve("c").isPresent());
    }

    @Test
    void functionSourceDelegatesToProvidedFunction() {
        PlaceholderSource source = PlaceholderSource.function(key -> Optional.of(key.toUpperCase()));
        assertEquals(Optional.of("HELLO"), source.resolve("hello"));
    }

    @Test
    void systemPropertiesSourceReadsJvmProperties() {
        System.setProperty("axiom.placeholder.test.prop", "value123");
        try {
            PlaceholderSource source = PlaceholderSource.systemProperties();
            assertEquals(Optional.of("value123"), source.resolve("axiom.placeholder.test.prop"));
        } finally {
            System.clearProperty("axiom.placeholder.test.prop");
        }
    }
}
