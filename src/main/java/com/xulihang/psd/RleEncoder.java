package com.xulihang.psd;

import java.util.Arrays;

/**
 * PackBits (RLE) encoder used by PSD channel data.
 */
final class RleEncoder {
    private RleEncoder() {
    }

    /**
     * Encodes one 8-bit channel as a complete PSD channel block:
     * 2-byte compression type, per-row byte counts, then packed rows.
     */
    static byte[] encodeChannel(byte[] channel, int width, int height) {
        byte[][] rows = encodeRows(channel, width, height);
        BinaryWriter writer = new BinaryWriter();
        writer.writeU16(1); // RLE
        for (byte[] row : rows) {
            writer.writeU16(row.length);
        }
        for (byte[] row : rows) {
            writer.writeBytes(row);
        }
        return writer.toByteArray();
    }

    /**
     * Encodes several 8-bit channels as a single PSD composite image block:
     * the row counts of all channels come first, then the packed data.
     */
    static byte[] encodeChannels(byte[][] channels, int width, int height) {
        byte[][][] rows = new byte[channels.length][][];
        for (int c = 0; c < channels.length; c++) {
            rows[c] = encodeRows(channels[c], width, height);
        }
        BinaryWriter writer = new BinaryWriter();
        writer.writeU16(1); // RLE
        for (byte[][] channelRows : rows) {
            for (byte[] row : channelRows) {
                writer.writeU16(row.length);
            }
        }
        for (byte[][] channelRows : rows) {
            for (byte[] row : channelRows) {
                writer.writeBytes(row);
            }
        }
        return writer.toByteArray();
    }

    private static byte[][] encodeRows(byte[] channel, int width, int height) {
        byte[][] rows = new byte[height][];
        for (int y = 0; y < height; y++) {
            rows[y] = packRow(channel, y * width, width);
        }
        return rows;
    }

    private static byte[] packRow(byte[] source, int offset, int length) {
        byte[] output = new byte[length + (length + 127) / 128 + 2];
        int end = offset + length;
        int in = offset;
        int out = 0;

        while (in < end) {
            int run = 1;
            while (in + run < end && run < 128 && source[in + run] == source[in]) {
                run++;
            }

            if (run > 1) {
                output[out++] = (byte) (1 - run);
                output[out++] = source[in];
                in += run;
            } else {
                int countIndex = out++;
                int count = 0;
                while (in < end && count < 128) {
                    if (in + 2 < end && source[in] == source[in + 1] && source[in] == source[in + 2]) {
                        break;
                    }
                    output[out++] = source[in++];
                    count++;
                }
                output[countIndex] = (byte) (count - 1);
            }
        }

        return Arrays.copyOf(output, out);
    }
}
