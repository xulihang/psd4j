package com.xulihang.psd;

/**
 * Paragraph alignment of a text layer.
 */
public enum TextAlignment {
    LEFT(0),
    RIGHT(1),
    CENTER(2);

    private final int justification;

    TextAlignment(int justification) {
        this.justification = justification;
    }

    int getJustification() {
        return justification;
    }
}
