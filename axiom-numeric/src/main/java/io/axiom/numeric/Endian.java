package io.axiom.numeric;

/** Byte order for multi-byte reads/writes on {@link Bytes} — always passed explicitly, never
 * defaulted, so a call site never leaves the byte order ambiguous. */
public enum Endian {
    BIG,
    LITTLE
}
