package io.axiom.console.source;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Reads from {@code System.in}; {@link #close()} deliberately never closes it. */
public final class ConsoleSource implements InputSource {

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    @Override
    public BufferedReader reader() {
        return reader;
    }

    @Override
    public void close() {
        // System.in is process-wide and outlives this handler; never close it here.
    }
}
