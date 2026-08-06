package io.axiom.id;

import io.axiom.id.internal.Crockford;
import io.axiom.id.internal.UlidGenerator;

import java.time.Instant;
import java.util.Arrays;

/**
 * A ULID (Universally Unique Lexicographically Sortable Identifier): a 128-bit value rendered as
 * a fixed 26-character Base32-Crockford string whose lexicographic order matches creation order.
 * Immutable; {@link #compareTo(Ulid)} follows the same order as {@link #toString()}.
 */
public final class Ulid implements Comparable<Ulid> {

    private final byte[] bytes;
    private final String text;

    private Ulid(byte[] bytes, String text) {
        this.bytes = bytes;
        this.text = text;
    }

    static Ulid fromBytes(byte[] bytes16) {
        return new Ulid(bytes16, Crockford.encode(bytes16));
    }

    /**
     * Parses a canonical 26-character ULID string.
     *
     * @throws IllegalArgumentException if {@code text} is not a well-formed ULID — same
     *     contract as {@link java.util.UUID#fromString(String)} for malformed input, so this
     *     module does not need an exception type of its own just for parse failures.
     */
    public static Ulid of(String text) {
        byte[] decoded = Crockford.decode(text);
        return new Ulid(decoded, Crockford.encode(decoded));
    }

    /** The 48-bit creation timestamp embedded in this ULID. */
    public Instant timestamp() {
        return Instant.ofEpochMilli(UlidGenerator.timestampMillis(bytes));
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int compareTo(Ulid other) {
        return text.compareTo(other.text);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Ulid other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
