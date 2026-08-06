package io.axiom.console.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/** Feeds predetermined input; the seam that makes {@code axiom-console} testable without {@code System.in}. */
public final class StringSource implements InputSource {

    private final BufferedReader reader;

    public StringSource(String content) {
        this.reader = new BufferedReader(new StringReader(content == null ? "" : content));
    }

    @Override
    public BufferedReader reader() {
        return reader;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }
}
