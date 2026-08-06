package io.axiom.console.source;

import java.io.BufferedReader;

/** Where the raw text a console session reads from actually comes from. */
public interface InputSource extends AutoCloseable {

    BufferedReader reader();

    @Override
    void close();
}
