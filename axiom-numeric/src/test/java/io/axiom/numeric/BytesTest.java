package io.axiom.numeric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BytesTest {

    @Test
    void writeThenReadU8() {
        Bytes bytes = Bytes.allocate(1).writeU8(0, 200);
        assertEquals(U8.of(200), bytes.readU8(0));
    }

    @Test
    void writeThenReadI8() {
        Bytes bytes = Bytes.allocate(1).writeI8(0, -50);
        assertEquals(I8.of(-50), bytes.readI8(0));
    }

    @Test
    void writeThenReadU16BigEndian() {
        Bytes bytes = Bytes.allocate(2).writeU16(0, 8080, Endian.BIG);
        assertEquals(U16.of(8080), bytes.readU16(0, Endian.BIG));
    }

    @Test
    void writeThenReadU16LittleEndian() {
        Bytes bytes = Bytes.allocate(2).writeU16(0, 8080, Endian.LITTLE);
        assertEquals(U16.of(8080), bytes.readU16(0, Endian.LITTLE));
    }

    @Test
    void bigAndLittleEndianProduceDifferentByteOrder() {
        Bytes big = Bytes.allocate(2).writeU16(0, 0x1234, Endian.BIG);
        Bytes little = Bytes.allocate(2).writeU16(0, 0x1234, Endian.LITTLE);
        assertEquals((byte) 0x12, big.toArray()[0]);
        assertEquals((byte) 0x34, little.toArray()[0]);
    }

    @Test
    void writeThenReadI32BothEndians() {
        Bytes bytes = Bytes.allocate(4).writeI32(0, -123456, Endian.LITTLE);
        assertEquals(I32.of(-123456), bytes.readI32(0, Endian.LITTLE));
    }

    @Test
    void writeThenReadU32BeyondIntRange() {
        Bytes bytes = Bytes.allocate(4).writeU32(0, 4_000_000_000L, Endian.BIG);
        assertEquals(U32.of(4_000_000_000L), bytes.readU32(0, Endian.BIG));
    }

    @Test
    void writeThenReadU64AndI64() {
        Bytes bytes = Bytes.allocate(16)
            .writeU64(0, -1L, Endian.BIG)
            .writeI64(8, Long.MIN_VALUE, Endian.LITTLE);
        assertEquals(U64.of(-1L), bytes.readU64(0, Endian.BIG));
        assertEquals(I64.of(Long.MIN_VALUE), bytes.readI64(8, Endian.LITTLE));
    }

    @Test
    void composedChainMatchesDocExample() {
        // Straight from docs/axiom-numeric.md's "Exemplo Rápido".
        Bytes packet = Bytes.allocate(8)
            .writeU8(0, 1)
            .writeU16(1, 8080, Endian.BIG);

        U8 version = packet.readU8(0);
        U16 port = packet.readU16(1, Endian.BIG);

        assertEquals(1, version.value());
        assertEquals(8080, port.value());
    }

    @Test
    void emptyBufferRejectsAnyAccess() {
        Bytes empty = Bytes.allocate(0);
        assertThrows(IndexOutOfBoundsException.class, () -> empty.writeU8(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.readU8(0));
    }

    @Test
    void outOfBoundsIndexThrows() {
        Bytes bytes = Bytes.allocate(2);
        assertThrows(IndexOutOfBoundsException.class, () -> bytes.writeU16(1, 100, Endian.BIG)); // needs 2 bytes from index 1
        assertThrows(IndexOutOfBoundsException.class, () -> bytes.readI32(0, Endian.BIG)); // needs 4 bytes
        assertThrows(IndexOutOfBoundsException.class, () -> bytes.writeU8(-1, 1));
    }
}
