package io.axiom.collections.hamt;

/**
 * Hash utilities used by the HAMT (Hash Array Mapped Trie) implementations in this module.
 *
 * <h2>Why mixing is required</h2>
 * Many {@code hashCode()} implementations are not uniformly distributed. This class applies
 * the MurmurHash3 32-bit finalization mix, which provides strong avalanche behavior at
 * minimal cost, to protect the trie against weak/collision-prone hash codes.
 *
 * <h2>Bit layout</h2>
 * HAMT splits a 32-bit hash into 5-bit segments (32-way branching factor); each trie level
 * consumes one segment via {@link #mask(int, int)}.
 */
public final class Hashing {

    private Hashing() {
    }

    public static int hash(Object o) {
        return mix(o == null ? 0 : o.hashCode());
    }

    /** MurmurHash3 32-bit finalizer. */
    public static int mix(int h) {
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return h;
    }

    /** Extracts the 5-bit segment (one HAMT trie level) at the given shift offset. */
    public static int mask(int hash, int shift) {
        return (hash >>> shift) & 0x1f;
    }

    /** Converts a 5-bit mask value into a bitmap with a single bit set. */
    public static int bitShift(int mask) {
        return 1 << mask;
    }

    /** Physical array index for a child, given the node's bitmap and the child's single-bit mask. */
    public static int index(int bitmap, int bitshift) {
        return Integer.bitCount(bitmap & (bitshift - 1));
    }
}
