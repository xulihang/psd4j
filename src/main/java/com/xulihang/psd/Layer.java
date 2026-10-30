package com.xulihang.psd;

import java.awt.image.BufferedImage;

/**
 * Base class for PSD layers.
 * <p>
 * Layers are ordered from bottom to top in {@link Psd}: the first added layer
 * is the bottom layer.
 */
public abstract class Layer {
    private String name = "Layer";
    private int left;
    private int top;
    private int opacity = 255;
    private boolean visible = true;
    private BlendMode blendMode = BlendMode.NORMAL;

    protected Layer() {
    }

    protected Layer(String name) {
        setName(name);
    }

    /**
     * Returns the raster content of the layer. For text layers this renders the
     * text with Java2D; the editable text itself is stored separately.
     */
    public abstract BufferedImage getImage();

    public String getName() {
        return name;
    }

    public Layer setName(String name) {
        this.name = name == null ? "" : name;
        return this;
    }

    public int getLeft() {
        return left;
    }

    public Layer setLeft(int left) {
        this.left = left;
        return this;
    }

    public int getTop() {
        return top;
    }

    public Layer setTop(int top) {
        this.top = top;
        return this;
    }

    public Layer setPosition(int left, int top) {
        this.left = left;
        this.top = top;
        return this;
    }

    /** Opacity in the range 0-255. */
    public int getOpacity() {
        return opacity;
    }

    public Layer setOpacity(int opacity) {
        this.opacity = Math.max(0, Math.min(255, opacity));
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public Layer setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public Layer setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode == null ? BlendMode.NORMAL : blendMode;
        return this;
    }
}
