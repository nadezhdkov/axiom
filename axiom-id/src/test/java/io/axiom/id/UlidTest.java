package io.axiom.id;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UlidTest {

    @Test
    void hasFixedLengthOfTwentySixCharacters() {
        assertEquals(26, Id.ulid().toString().length());
    }

    @Test
    void usesOnlyCrockfordBase32Alphabet() {
        String text = Id.ulid().toString();
        assertTrue(text.chars().allMatch(c ->
            Character.isDigit(c) || (Character.isUpperCase(c) && "ILOU".indexOf(c) < 0)));
    }

    @Test
    void embeddedTimestampIsPlausible() {
        long before = System.currentTimeMillis();
        Ulid ulid = Id.ulid();
        long after = System.currentTimeMillis();

        long millis = ulid.timestamp().toEpochMilli();
        assertTrue(millis >= before && millis <= after,
            "embedded timestamp " + Instant.ofEpochMilli(millis) + " outside generation window");
    }

    @Test
    void roundTripsThroughParse() {
        Ulid original = Id.ulid();
        Ulid parsed = Ulid.of(original.toString());
        assertEquals(original, parsed);
        assertEquals(original.toString(), parsed.toString());
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> Ulid.of("TOOSHORT"));
    }

    @Test
    void rejectsInvalidCharacters() {
        // 'I', 'L', 'O', 'U' are deliberately excluded from the Crockford alphabet.
        assertThrows(IllegalArgumentException.class, () -> Ulid.of("IIIIIIIIIIIIIIIIIIIIIIIIII"));
    }

    @Test
    void backToBackCallsAreMonotonicallySortable() {
        Ulid[] ids = new Ulid[1000];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = Id.ulid();
        }
        for (int i = 1; i < ids.length; i++) {
            assertTrue(ids[i - 1].compareTo(ids[i]) < 0,
                "expected " + ids[i - 1] + " < " + ids[i]);
        }
    }

    @Test
    void lexicographicOrderMatchesNaturalOrder() {
        Ulid a = Id.ulid();
        Ulid b = Id.ulid();
        int naturalOrder = Integer.signum(a.compareTo(b));
        int lexicographicOrder = Integer.signum(a.toString().compareTo(b.toString()));
        assertEquals(lexicographicOrder, naturalOrder);
    }

    @Test
    void generatesUniqueValuesAtVolume() {
        Set<Ulid> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertTrue(seen.add(Id.ulid()), "duplicate ULID generated");
        }
    }
}
