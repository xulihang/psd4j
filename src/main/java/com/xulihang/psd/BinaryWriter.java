package com.xulihang.psd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Growable big-endian byte writer with the small set of primitives needed by
 * the PSD format.
 */
final class BinaryWriter {
    private static final Set<String> STRING_CLASS_IDS =
            new HashSet<>(Arrays.asList("warp", "time", "hold", "list", "json"));

    private byte[] buffer = new byte[4096];
    private int size;

    int size() {
        return size;
    }

    byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    private void ensure(int extra) {
        if (size + extra <= buffer.length) {
            return;
        }
        int newLength = Math.max(buffer.length * 2, size + extra);
        buffer = Arrays.copyOf(buffer, newLength);
    }

    void writeByte(int value) {
        ensure(1);
        buffer[size++] = (byte) value;
    }

    void writeBytes(byte[] bytes) {
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buffer, size, bytes.length);
        size += bytes.length;
    }

    void writeZeros(int count) {
        ensure(count);
        size += count;
    }

    void writeU16(int value) {
        ensure(2);
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) value;
    }

    void writeI16(int value) {
        writeU16(value & 0xffff);
    }

    void writeI32(int value) {
        ensure(4);
        buffer[size++] = (byte) (value >>> 24);
        buffer[size++] = (byte) (value >>> 16);
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) value;
    }

    void writeU32(long value) {
        ensure(4);
        buffer[size++] = (byte) (value >>> 24);
        buffer[size++] = (byte) (value >>> 16);
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) value;
    }

    void writeF32(float value) {
        writeI32(Float.floatToIntBits(value));
    }

    void writeF64(double value) {
        long bits = Double.doubleToLongBits(value);
        ensure(8);
        for (int shift = 56; shift >= 0; shift -= 8) {
            buffer[size++] = (byte) (bits >>> shift);
        }
    }

    void writeSignature(String signature) {
        if (signature.length() != 4) {
            throw new IllegalArgumentException("Invalid signature: " + signature);
        }
        for (int i = 0; i < 4; i++) {
            writeByte(signature.charAt(i));
        }
    }

    void writeAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            writeByte(c < 256 ? c : '?');
        }
    }

    /** Pascal string padded so that the length byte + text + padding is a multiple of padTo. */
    void writePascalString(String text, int padTo) {
        int length = Math.min(text.length(), 255);
        writeByte(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            writeByte(c < 128 ? c : '?');
        }
        int written = length + 1;
        while (written % padTo != 0) {
            writeByte(0);
            written++;
        }
    }

    /** 4-byte character count followed by UTF-16BE characters. */
    void writeUnicodeString(String text) {
        writeU32(text.length());
        writeUtf16(text);
    }

    /** Unicode string with the trailing null character Photoshop uses in descriptors. */
    void writeUnicodeStringWithPadding(String text) {
        writeU32(text.length() + 1);
        writeUtf16(text);
        writeU16(0);
    }

    void writeUtf16(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            writeU16(c);
        }
    }

    /**
     * Some descriptor identifiers are stored as a class ID (length 0 + 4 chars)
     * while others are stored as a length-prefixed ASCII string.
     */
    void writeAsciiStringOrClassId(String value) {
        if (value.length() == 4 && !STRING_CLASS_IDS.contains(value)) {
            writeU32(0);
            writeSignature(value);
        } else {
            writeU32(value.length());
            writeAscii(value);
        }
    }

    interface SectionBody {
        void write(BinaryWriter writer);
    }

    /**
     * Writes a length-prefixed, padded section. When includePaddingInLength is
     * true the recorded length covers the padding bytes too (PSD layer info).
     */
    static byte[] section(int round, boolean includePaddingInLength, SectionBody body) {
        BinaryWriter inner = new BinaryWriter();
        body.write(inner);
        int length = inner.size();
        int padded = length;
        while (padded % round != 0) {
            padded++;
        }
        BinaryWriter out = new BinaryWriter();
        out.writeU32(includePaddingInLength ? padded : length);
        out.writeBytes(inner.toByteArray());
        out.writeZeros(padded - length);
        return out.toByteArray();
    }
}
