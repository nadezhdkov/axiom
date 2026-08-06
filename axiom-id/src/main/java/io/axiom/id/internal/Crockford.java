package io.axiom.id.internal;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Base32-Crockford encoding/decoding, fixed to the 26-character ULID alphabet (no I/L/O/U, to
 * avoid visual ambiguity). Not exported — {@link io.axiom.id.Ulid} is the only public surface.
 */
public final class Crockford {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int[] DECODE = new int[128];

    static {
        Arrays.fill(DECODE, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            DECODE[ALPHABET[i]] = i;
            DECODE[Character.toLowerCase(ALPHABET[i])] = i;
        }
    }

    private Crockford() {
    }

    /** Encodes 16 bytes (128 bits: 48-bit timestamp + 80-bit randomness) into 26 characters. */
    public static String encode(byte[] bytes16) {
        if (bytes16.length != 16) {
            throw new IllegalArgumentException("expected 16 bytes, got " + bytes16.length);
        }
        BigInteger value = new BigInteger(1, bytes16);
        char[] out = new char[26];
        for (int i = 25; i >= 0; i--) {
            int digit = value.and(BigInteger.valueOf(0x1F)).intValue();
            out[i] = ALPHABET[digit];
            value = value.shiftRight(5);
        }
        return new String(out);
    }

    /** Decodes a 26-character Crockford string back into 16 bytes. */
    public static byte[] decode(String text) {
        if (text == null || text.length() != 26) {
            throw new IllegalArgumentException("ULID must be 26 characters, got: " + text);
        }
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < 26; i++) {
            char c = text.charAt(i);
            int digit = c < DECODE.length ? DECODE[c] : -1;
            if (digit < 0) {
                throw new IllegalArgumentException("invalid Crockford Base32 character: '" + c + "'");
            }
            value = value.shiftLeft(5).or(BigInteger.valueOf(digit));
        }
        byte[] full = value.toByteArray();
        byte[] result = new byte[16];
        int copyLen = Math.min(full.length, 16);
        System.arraycopy(full, full.length - copyLen, result, 16 - copyLen, copyLen);
        return result;
    }
}
