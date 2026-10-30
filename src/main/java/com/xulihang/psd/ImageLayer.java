package com.xulihang.psd;

import java.awt.image.BufferedImage;

/**
 * A raster (pixel) layer backed by a {@link BufferedImage}.
 */
public final class ImageLayer extends Layer {
    private BufferedImage image;

    public ImageLayer(BufferedImage image) {
        this("Layer", image, 0, 0);
    }

    public ImageLayer(String name, BufferedImage image) {
        this(name, image, 0, 0);
    }

    public ImageLayer(BufferedImage image, int left, int top) {
        this("Layer", image, left, top);
    }

    public ImageLayer(String name, BufferedImage image, int left, int top) {
        super(name);
        this.image = image;
        setPosition(left, top);
    }

    @Override
    public BufferedImage getImage() {
        return image;
    }

    public ImageLayer setImage(BufferedImage image) {
        this.image = image;
        return this;
    }
}
