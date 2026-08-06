package io.axiom.numeric;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A fixed-size, fluent byte buffer for binary protocols/formats. Delegates the actual
 * byte-order-aware encoding to {@link java.nio.ByteBuffer#order(ByteOrder)} internally rather
 * than hand-rolled bit-shifting — the JDK already got this right.
 */
public final class Bytes {

    private final byte[] data;

    private Bytes(byte[] data) {
        this.data = data;
    }

    public static Bytes allocate(int size) {
        return new Bytes(new byte[size]);
    }

    public int length() {
        return data.length;
    }

    public byte[] toArray() {
        return data.clone();
    }

    public Bytes writeU8(int index, int value) {
        checkRange(index, 1);
        data[index] = (byte) U8.of(value).value();
        return this;
    }

    public Bytes writeI8(int index, int value) {
        checkRange(index, 1);
        data[index] = (byte) I8.of(value).value();
        return this;
    }

    public Bytes writeU16(int index, int value, Endian endian) {
        return writeShort(index, (short) U16.of(value).value(), endian);
    }

    public Bytes writeI16(int index, int value, Endian endian) {
        return writeShort(index, (short) I16.of(value).value(), endian);
    }

    public Bytes writeU32(int index, long value, Endian endian) {
        return writeInt(index, (int) U32.of(value).value(), endian);
    }

    public Bytes writeI32(int index, int value, Endian endian) {
        return writeInt(index, I32.of(value).value(), endian);
    }

    public Bytes writeU64(int index, long value, Endian endian) {
        return writeLong(index, U64.of(value).value(), endian);
    }

    public Bytes writeI64(int index, long value, Endian endian) {
        return writeLong(index, I64.of(value).value(), endian);
    }

    public U8 readU8(int index) {
        checkRange(index, 1);
        return U8.of(data[index] & 0xFF);
    }

    public I8 readI8(int index) {
        checkRange(index, 1);
        return I8.of(data[index]);
    }

    public U16 readU16(int index, Endian endian) {
        return U16.of(readShort(index, endian) & 0xFFFF);
    }

    public I16 readI16(int index, Endian endian) {
        return I16.of(readShort(index, endian));
    }

    public U32 readU32(int index, Endian endian) {
        return U32.of(Integer.toUnsignedLong(readInt(index, endian)));
    }

    public I32 readI32(int index, Endian endian) {
        return I32.of(readInt(index, endian));
    }

    public U64 readU64(int index, Endian endian) {
        return U64.of(readLong(index, endian));
    }

    public I64 readI64(int index, Endian endian) {
        return I64.of(readLong(index, endian));
    }

    private Bytes writeShort(int index, short value, Endian endian) {
        checkRange(index, 2);
        ByteBuffer.wrap(data, index, 2).order(order(endian)).putShort(value);
        return this;
    }

    private Bytes writeInt(int index, int value, Endian endian) {
        checkRange(index, 4);
        ByteBuffer.wrap(data, index, 4).order(order(endian)).putInt(value);
        return this;
    }

    private Bytes writeLong(int index, long value, Endian endian) {
        checkRange(index, 8);
        ByteBuffer.wrap(data, index, 8).order(order(endian)).putLong(value);
        return this;
    }

    private short readShort(int index, Endian endian) {
        checkRange(index, 2);
        return ByteBuffer.wrap(data, index, 2).order(order(endian)).getShort();
    }

    private int readInt(int index, Endian endian) {
        checkRange(index, 4);
        return ByteBuffer.wrap(data, index, 4).order(order(endian)).getInt();
    }

    private long readLong(int index, Endian endian) {
        checkRange(index, 8);
        return ByteBuffer.wrap(data, index, 8).order(order(endian)).getLong();
    }

    private static ByteOrder order(Endian endian) {
        return endian == Endian.BIG ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }

    private void checkRange(int index, int width) {
        if (index < 0 || index + width > data.length) {
            throw new IndexOutOfBoundsException(
                "index " + index + " with width " + width + " out of bounds for Bytes of length " + data.length);
        }
    }
}
