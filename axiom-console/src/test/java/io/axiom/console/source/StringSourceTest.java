package io.axiom.console.source;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringSourceTest {

    @Test
    void readsLinesInOrder() throws IOException {
        StringSource source = new StringSource("a\nb\nc");
        assertEquals("a", source.reader().readLine());
        assertEquals("b", source.reader().readLine());
        assertEquals("c", source.reader().readLine());
        assertNull(source.reader().readLine());
    }

    @Test
    void nullContentBehavesAsEmptyInput() throws IOException {
        StringSource source = new StringSource(null);
        assertNull(source.reader().readLine());
    }
}
