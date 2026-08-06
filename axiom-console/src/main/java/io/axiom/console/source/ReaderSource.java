package io.axiom.console.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class ReaderSource implements InputSource {

    private final BufferedReader reader;

    public ReaderSource(Reader reader) {
        this.reader = new BufferedReader(reader);
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
