package io.axiom.numeric.examples;

import io.axiom.numeric.Bytes;
import io.axiom.numeric.Endian;
import io.axiom.numeric.I32;
import io.axiom.numeric.I8;
import io.axiom.numeric.U16;
import io.axiom.numeric.U32;
import io.axiom.numeric.U8;

public final class NumericExamples {

    private NumericExamples() {
    }

    public static void main(String[] args) {
        U8 version = U8.of(1);
        U16 port = U16.of(8080);
        U32 packetLength = U32.of(4_000_000_000L);
        System.out.println("version=" + version.value() + " port=" + port.value() + " length=" + packetLength.value());

        I32 value = I32.of(300);
        System.out.println("300 -> I8 wrapping: " + value.toI8Wrapping().value());
        System.out.println("300 -> I8 saturated: " + value.toI8Saturated().value());
        try {
            value.toI8Checked();
        } catch (ArithmeticException e) {
            System.out.println("300 -> I8 checked: " + e.getMessage());
        }

        Bytes packet = Bytes.allocate(8)
            .writeU8(0, 1)
            .writeU16(1, 8080, Endian.BIG);

        U8 readVersion = packet.readU8(0);
        U16 readPort = packet.readU16(1, Endian.BIG);
        System.out.println("read back: version=" + readVersion.value() + " port=" + readPort.value());

        I8 flags = I8.of(0).setBit(0).setBit(2);
        System.out.println("flags=" + flags.value() + " hasBit(2)=" + flags.hasBit(2));
    }
}
