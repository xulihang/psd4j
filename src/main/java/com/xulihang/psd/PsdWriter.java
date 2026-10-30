package com.xulihang.psd;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializes a {@link Psd} to the PSD file format (8-bit RGB, version 1).
 */
final class PsdWriter {
    private static final int COLOR_MODE_RGB = 3;
    private static final int CHANNEL_ALPHA = -1;
    private static final int CHANNEL_RED = 0;
    private static final int CHANNEL_GREEN = 1;
    private static final int CHANNEL_BLUE = 2;

    private PsdWriter() {
    }

    static byte[] write(Psd psd) {
        int width = psd.getWidth();
        int height = psd.getHeight();

        BufferedImage composite = psd.getCompositeImage();
        if (composite.getWidth() != width || composite.getHeight() != height) {
            throw new IllegalArgumentException("Composite image must have the same size as the document");
        }

        int[] compositePixels = composite.getRGB(0, 0, width, height, null, 0, width);
        boolean globalAlpha = hasAlpha(compositePixels);

        List<LayerData> layerData = new ArrayList<>();
        for (Layer layer : psd.getLayers()) {
            layerData.add(buildLayerData(layer));
        }
        if (layerData.isEmpty()) {
            layerData.add(emptyLayerData());
        }

        BinaryWriter out = new BinaryWriter();

        // File header
        out.writeSignature("8BPS");
        out.writeU16(1); // version 1 (PSD)
        out.writeZeros(6); // reserved
        out.writeU16(globalAlpha ? 4 : 3); // channels in the composite
        out.writeU32(height);
        out.writeU32(width);
        out.writeU16(8); // bits per channel
        out.writeU16(COLOR_MODE_RGB);

        // Color mode data (empty for RGB)
        out.writeBytes(BinaryWriter.section(1, false, writer -> {
        }));

        // Image resources (none)
        out.writeBytes(BinaryWriter.section(2, false, writer -> {
        }));

        // Layer and mask information
        byte[] layerInfo = BinaryWriter.section(4, true,
                writer -> writeLayerInfo(writer, layerData, globalAlpha));
        byte[] globalLayerMaskInfo = BinaryWriter.section(4, true, writer -> {
        });
        byte[] layerAndMaskInfo = BinaryWriter.section(2, false, writer -> {
            writer.writeBytes(layerInfo);
            writer.writeBytes(globalLayerMaskInfo);
        });
        out.writeBytes(layerAndMaskInfo);

        // Composite image data
        out.writeBytes(encodeComposite(compositePixels, width, height, globalAlpha));

        return out.toByteArray();
    }

    private static void writeLayerInfo(
            BinaryWriter out, List<LayerData> layers, boolean globalAlpha) {
        // A negative count means the first alpha channel contains the transparency data.
        out.writeI16(globalAlpha ? -layers.size() : layers.size());

        for (LayerData data : layers) {
            writeLayerRecord(out, data);
        }

        for (LayerData data : layers) {
            out.writeBytes(data.alphaChannel);
            out.writeBytes(data.redChannel);
            out.writeBytes(data.greenChannel);
            out.writeBytes(data.blueChannel);
        }
    }

    private static void writeLayerRecord(BinaryWriter out, LayerData data) {
        out.writeI32(data.top);
        out.writeI32(data.left);
        out.writeI32(data.bottom);
        out.writeI32(data.right);

        out.writeU16(4); // channel count
        out.writeI16(CHANNEL_ALPHA);
        out.writeU32(data.alphaChannel.length);
        out.writeI16(CHANNEL_RED);
        out.writeU32(data.redChannel.length);
        out.writeI16(CHANNEL_GREEN);
        out.writeU32(data.greenChannel.length);
        out.writeI16(CHANNEL_BLUE);
        out.writeU32(data.blueChannel.length);

        out.writeSignature("8BIM");
        out.writeSignature(data.layer.getBlendMode().getKey());
        out.writeByte(data.layer.getOpacity());
        out.writeByte(0); // clipping
        int flags = 0x08; // bit 3: flag byte contains useful information
        if (!data.layer.isVisible()) {
            flags |= 0x02;
        }
        out.writeByte(flags);
        out.writeByte(0); // filler

        byte[] extra = buildExtraData(data);
        out.writeBytes(BinaryWriter.section(1, false, writer -> writer.writeBytes(extra)));
    }

    private static byte[] buildExtraData(LayerData data) {
        BinaryWriter out = new BinaryWriter();

        out.writeU32(0); // layer mask data length
        out.writeU32(0); // blending ranges length
        out.writePascalString(data.layer.getName(), 4);

        BinaryWriter unicodeName = new BinaryWriter();
        unicodeName.writeUnicodeString(data.layer.getName());
        out.writeBytes(additionalInfo("luni", 4, unicodeName.toByteArray()));

        if (data.tySh != null) {
            out.writeBytes(additionalInfo("TySh", 2, data.tySh));
        }

        return out.toByteArray();
    }

    private static byte[] additionalInfo(String key, int round, byte[] payload) {
        int padded = payload.length;
        while (padded % round != 0) {
            padded++;
        }
        BinaryWriter out = new BinaryWriter();
        out.writeSignature("8BIM");
        out.writeSignature(key);
        out.writeU32(padded);
        out.writeBytes(payload);
        out.writeZeros(padded - payload.length);
        return out.toByteArray();
    }

    private static LayerData buildLayerData(Layer layer) {
        LayerData data = new LayerData();
        data.layer = layer;
        data.top = layer.getTop();
        data.left = layer.getLeft();

        BufferedImage image = layer.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            data.right = data.left;
            data.bottom = data.top;
            data.alphaChannel = emptyChannel();
            data.redChannel = emptyChannel();
            data.greenChannel = emptyChannel();
            data.blueChannel = emptyChannel();
            return data;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        data.right = data.left + width;
        data.bottom = data.top + height;

        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        byte[] alpha = new byte[pixels.length];
        byte[] red = new byte[pixels.length];
        byte[] green = new byte[pixels.length];
        byte[] blue = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            alpha[i] = (byte) (pixel >>> 24);
            red[i] = (byte) (pixel >>> 16);
            green[i] = (byte) (pixel >>> 8);
            blue[i] = (byte) pixel;
        }

        data.alphaChannel = RleEncoder.encodeChannel(alpha, width, height);
        data.redChannel = RleEncoder.encodeChannel(red, width, height);
        data.greenChannel = RleEncoder.encodeChannel(green, width, height);
        data.blueChannel = RleEncoder.encodeChannel(blue, width, height);

        if (layer instanceof TextLayer) {
            data.tySh = TyShWriter.write((TextLayer) layer, width, height);
        }

        return data;
    }

    private static LayerData emptyLayerData() {
        LayerData data = new LayerData();
        data.layer = new EmptyLayer();
        data.top = 0;
        data.left = 0;
        data.right = 0;
        data.bottom = 0;
        data.alphaChannel = emptyChannel();
        data.redChannel = emptyChannel();
        data.greenChannel = emptyChannel();
        data.blueChannel = emptyChannel();
        return data;
    }

    private static byte[] emptyChannel() {
        BinaryWriter out = new BinaryWriter();
        out.writeU16(0); // raw compression, no pixel data
        return out.toByteArray();
    }

    private static boolean hasAlpha(int[] pixels) {
        for (int pixel : pixels) {
            if ((pixel >>> 24) != 0xff) {
                return true;
            }
        }
        return false;
    }

    private static byte[] encodeComposite(
            int[] pixels, int width, int height, boolean globalAlpha) {
        int size = width * height;
        byte[] red = new byte[size];
        byte[] green = new byte[size];
        byte[] blue = new byte[size];
        byte[] alpha = new byte[size];

        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];
            int a = pixel >>> 24;
            int r = (pixel >>> 16) & 0xff;
            int g = (pixel >>> 8) & 0xff;
            int b = pixel & 0xff;
            alpha[i] = (byte) a;

            if (globalAlpha && a != 0 && a != 255) {
                // Photoshop stores the composite matted against white.
                double af = a / 255.0;
                double white = 255 * (1 - af);
                r = (int) (r * af + white);
                g = (int) (g * af + white);
                b = (int) (b * af + white);
            }

            red[i] = (byte) r;
            green[i] = (byte) g;
            blue[i] = (byte) b;
        }

        byte[][] channels = globalAlpha
                ? new byte[][]{red, green, blue, alpha}
                : new byte[][]{red, green, blue};
        return RleEncoder.encodeChannels(channels, width, height);
    }

    private static final class LayerData {
        Layer layer;
        int top;
        int left;
        int bottom;
        int right;
        byte[] alphaChannel;
        byte[] redChannel;
        byte[] greenChannel;
        byte[] blueChannel;
        byte[] tySh;
    }

    private static final class EmptyLayer extends Layer {
        @Override
        public BufferedImage getImage() {
            return null;
        }
    }
}
