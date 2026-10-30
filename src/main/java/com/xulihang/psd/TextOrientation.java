package com.xulihang.psd;

/**
 * Text flow direction of a text layer: horizontal (left to right) or vertical
 * (top to bottom, columns advancing right to left).
 */
public enum TextOrientation {
    HORIZONTAL("Hrzn", 0),
    VERTICAL("Vrtc", 2);

    private final String enumValue;
    private final int writingDirection;

    TextOrientation(String enumValue, int writingDirection) {
        this.enumValue = enumValue;
        this.writingDirection = writingDirection;
    }

    /** Value of the TySh "Ornt" enum field. */
    String getEnumValue() {
        return enumValue;
    }

    /** Value of the EngineData "WritingDirection" field. */
    int getWritingDirection() {
        return writingDirection;
    }

    /** Value of the EngineData "Procession" field. */
    int getProcession() {
        return this == VERTICAL ? 1 : 0;
    }

    boolean isVertical() {
        return this == VERTICAL;
    }
}
