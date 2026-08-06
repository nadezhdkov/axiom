package io.axiom.core.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HumanDurationTest {

    @Test
    void formatsOnlyNonZeroUnitsLargestToSmallest() {
        assertEquals("2h 30m", HumanDuration.format(Duration.ofMinutes(150)));
        assertEquals("1d 30m", HumanDuration.format(Duration.ofDays(1).plusMinutes(30)));
    }

    @Test
    void formatsZeroDurationAsZeroSeconds() {
        assertEquals("0s", HumanDuration.format(Duration.ZERO));
    }

    @Test
    void formatsSubMinuteDurationAsSeconds() {
        assertEquals("45s", HumanDuration.format(Duration.ofSeconds(45)));
    }

    @Test
    void parseRoundTripsWithFormat() {
        Duration original = Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(15);
        assertEquals(original, HumanDuration.parse(HumanDuration.format(original)));
    }

    @Test
    void parseAcceptsAnySubsetAndOrderOfComponents() {
        assertEquals(Duration.ofMinutes(150), HumanDuration.parse("30m 2h"));
    }

    @Test
    void parseThrowsOnGarbageInput() {
        assertThrows(IllegalArgumentException.class, () -> HumanDuration.parse("not a duration"));
    }

    @Test
    void parseThrowsOnEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> HumanDuration.parse("   "));
    }
}
