package com.xulihang.psd;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PsdTest {
    @Test
    public void writesHeaderImageLayerAndTextLayer() {
        Psd psd = new Psd(200, 100);
        BufferedImage background = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        psd.addImageLayer("背景", background, 0, 0);
        psd.addTextLayer(new TextLayer("标题", "Hello\n你好", 10, 20)
                .setFontSize(24)
                .setFontName("MicrosoftYaHei"));

        byte[] data = psd.toByteArray();

        assertEquals("8BPS", new String(data, 0, 4, StandardCharsets.ISO_8859_1));
        assertEquals(3, readU16(data, 12)); // opaque composite: RGB
        assertEquals(100, readI32(data, 14));
        assertEquals(200, readI32(data, 18));
        assertEquals(8, readU16(data, 22));
        assertEquals(3, readU16(data, 24)); // RGB color mode

        String binary = new String(data, StandardCharsets.ISO_8859_1);
        assertTrue(binary.contains("TySh"));
        assertTrue(binary.contains("luni"));
        assertTrue(binary.contains("EngineDict"));
    }

    @Test
    public void transparentDocumentUsesAlphaChannelAndNegativeLayerCount() {
        Psd psd = new Psd(10, 10);
        psd.addTextLayer(new TextLayer("Hello", 0, 0).setFontSize(6).setBox(10, 10));

        byte[] data = psd.toByteArray();

        assertEquals(4, readU16(data, 12)); // RGBA composite
        // header (26) + color mode data (4) + image resources (4) + layer/mask length (4)
        // + layer info length (4) -> layer count
        assertTrue(readI16(data, 42) < 0);
    }

    @Test
    public void verticalTextWritesVerticalOrientation() {
        Psd vertical = new Psd(100, 100);
        vertical.addTextLayer(new TextLayer("竖排", "中\n文", 10, 10).setOrientation(TextOrientation.VERTICAL));
        String verticalData = new String(vertical.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(verticalData.contains("Vrtc"));

        Psd horizontal = new Psd(100, 100);
        horizontal.addTextLayer(new TextLayer("横排", "中\n文", 10, 10));
        String horizontalData = new String(horizontal.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(horizontalData.contains("Hrzn"));
    }

    @Test
    public void verticalTextPreviewStacksCharactersInColumns() {
        BufferedImage image = new TextLayer("竖排", "中文汉", 0, 0)
                .setOrientation(TextOrientation.VERTICAL)
                .getImage();

        // three characters in one column: taller than wide
        assertTrue(image.getHeight() > image.getWidth());
        // column is wider than a single glyph, i.e. the line was not drawn as horizontal text
        int glyphWidth = new TextLayer("中", 0, 0).getImage().getWidth();
        assertTrue(image.getWidth() > glyphWidth);

    private static int readU16(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
    }

    private static int readI16(byte[] data, int offset) {
        return (short) readU16(data, offset);
    }

    private static int readI32(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24)
                | ((data[offset + 1] & 0xff) << 16)
                | ((data[offset + 2] & 0xff) << 8)
                | (data[offset + 3] & 0xff);
    }
}
