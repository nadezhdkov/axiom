package io.axiom.csv;

/**
 * Immutable parsing/writing configuration. Header handling is opt-in and explicit
 * ({@link #withHeader(boolean)}) rather than auto-detected — no implicit behavior, same
 * discipline applied to {@code axiom-numeric}'s explicit {@code Endian} on every read/write.
 */
public final class CsvConfig {

    private final char delimiter;
    private final char quote;
    private final boolean header;

    private CsvConfig(char delimiter, char quote, boolean header) {
        this.delimiter = delimiter;
        this.quote = quote;
        this.header = header;
    }

    public static CsvConfig defaults() {
        return new CsvConfig(',', '"', false);
    }

    public CsvConfig withDelimiter(char delimiter) {
        return new CsvConfig(delimiter, this.quote, this.header);
    }

    public CsvConfig withQuote(char quote) {
        return new CsvConfig(this.delimiter, quote, this.header);
    }

    public CsvConfig withHeader(boolean header) {
        return new CsvConfig(this.delimiter, this.quote, header);
    }

    public char delimiter() {
        return delimiter;
    }

    public char quote() {
        return quote;
    }

    public boolean header() {
        return header;
    }
}
