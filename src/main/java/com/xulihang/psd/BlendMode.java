package com.xulihang.psd;

/**
 * Layer blend modes supported by the PSD format.
 */
public enum BlendMode {
    NORMAL("norm"),
    DARKEN("dark"),
    MULTIPLY("mul "),
    COLOR_BURN("idiv"),
    LINEAR_BURN("lbrn"),
    LIGHTEN("lite"),
    SCREEN("scrn"),
    COLOR_DODGE("div "),
    LINEAR_DODGE("lddg"),
    OVERLAY("over"),
    SOFT_LIGHT("sLit"),
    HARD_LIGHT("hLit"),
    VIVID_LIGHT("vLit"),
    LINEAR_LIGHT("lLit"),
    PIN_LIGHT("pLit"),
    HARD_MIX("hMix"),
    DIFFERENCE("diff"),
    EXCLUSION("smud"),
    SUBTRACT("fsub"),
    DIVIDE("fdiv"),
    HUE("hue "),
    SATURATION("sat "),
    COLOR("colr"),
    LUMINOSITY("lum ");

    private final String key;

    BlendMode(String key) {
        this.key = key;
    }

    String getKey() {
        return key;
    }
}
