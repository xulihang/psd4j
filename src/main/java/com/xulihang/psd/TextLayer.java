package com.xulihang.psd;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * An editable text layer. The text itself is stored in the PSD TySh record;
 * a Java2D-rendered bitmap is attached as the layer pixels so the layer has a
 * preview before Photoshop re-renders it.
 */
public final class TextLayer extends Layer {
    private String text;
    private String fontFamily = "Arial";
    private String fontName;
    private double fontSize = 24;
    private Color color = Color.BLACK;
    private boolean bold;
    private boolean italic;
    private TextAlignment alignment = TextAlignment.LEFT;
    private int boxWidth;
    private int boxHeight;
    private double lineHeight;
    private String antiAlias = "sharp";

    private BufferedImage rendered;

    public TextLayer(String text) {
        this(text, 0, 0);
    }

    public TextLayer(String text, int left, int top) {
        this(null, text, left, top);
    }

    /**
     * @param name layer name; when null a name is derived from the text
     */
    public TextLayer(String name, String text, int left, int top) {
        super(name != null ? name : defaultName(text));
        this.text = text == null ? "" : text;
        setPosition(left, top);
    }

    private static String defaultName(String text) {
        String oneLine = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return oneLine.isEmpty() ? "Text" : "Text: " + oneLine.substring(0, Math.min(24, oneLine.length()));
    }

    @Override
    public BufferedImage getImage() {
        if (rendered == null) {
            rendered = render();
        }
        return rendered;
    }

    public String getText() {
        return text;
    }

    public TextLayer setText(String text) {
        this.text = text == null ? "" : text;
        invalidate();
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public TextLayer setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily == null || fontFamily.isEmpty() ? "Arial" : fontFamily;
        invalidate();
        return this;
    }

    /**
     * PostScript font name stored in the PSD (for example "MicrosoftYaHei").
     * Defaults to the font family.
     */
    public String getFontName() {
        return fontName == null || fontName.isEmpty() ? fontFamily : fontName;
    }

    public TextLayer setFontName(String fontName) {
        this.fontName = fontName;
        return this;
    }

    /** Font size in points (1 point = 1 pixel at 72 DPI). */
    public double getFontSize() {
        return fontSize;
    }

    public TextLayer setFontSize(double fontSize) {
        this.fontSize = Math.max(1, fontSize);
        invalidate();
        return this;
    }

    public Color getColor() {
        return color;
    }

    public TextLayer setColor(Color color) {
        this.color = color == null ? Color.BLACK : color;
        invalidate();
        return this;
    }

    public boolean isBold() {
        return bold;
    }

    public TextLayer setBold(boolean bold) {
        this.bold = bold;
        invalidate();
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public TextLayer setItalic(boolean italic) {
        this.italic = italic;
        invalidate();
        return this;
    }

    public TextAlignment getAlignment() {
        return alignment;
    }

    public TextLayer setAlignment(TextAlignment alignment) {
        this.alignment = alignment == null ? TextAlignment.LEFT : alignment;
        invalidate();
        return this;
    }

    /**
     * Text box size in pixels. A width or height of 0 lets the library measure
     * the text and size the box to fit it.
     */
    public TextLayer setBox(int width, int height) {
        this.boxWidth = Math.max(0, width);
        this.boxHeight = Math.max(0, height);
        invalidate();
        return this;
    }

    public int getBoxWidth() {
        return boxWidth;
    }

    public int getBoxHeight() {
        return boxHeight;
    }

    /** Line spacing in pixels; 0 uses the font's natural line height. */
    public double getLineHeight() {
        return lineHeight;
    }

    public TextLayer setLineHeight(double lineHeight) {
        this.lineHeight = Math.max(0, lineHeight);
        invalidate();
        return this;
    }

    /**
     * Anti-aliasing mode stored in the PSD: none, crisp, strong, smooth, sharp,
     * platform or platformLCD.
     */
    public String getAntiAlias() {
        return antiAlias;
    }

    public TextLayer setAntiAlias(String antiAlias) {
        this.antiAlias = antiAlias == null ? "sharp" : antiAlias;
        return this;
    }

    private void invalidate() {
        rendered = null;
    }

    private BufferedImage render() {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        Font font = new Font(fontFamily, fontStyle(), 1).deriveFont((float) fontSize);

        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measuring = scratch.createGraphics();
        measuring.setFont(font);
        FontMetrics metrics = measuring.getFontMetrics();
        measuring.dispose();

        int lineHeightPx = lineHeight > 0 ? (int) Math.ceil(lineHeight) : metrics.getHeight();
        int contentWidth = 0;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, metrics.stringWidth(line));
        }

        int width = boxWidth > 0 ? boxWidth : Math.max(1, contentWidth + 2);
        int height = boxHeight > 0 ? boxHeight : Math.max(1, lineHeightPx * lines.length + 2);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setColor(color);

        int y = metrics.getAscent() + 1;
        for (String line : lines) {
            int textWidth = metrics.stringWidth(line);
            int x;
            if (alignment == TextAlignment.CENTER) {
                x = Math.max(0, (width - textWidth) / 2);
            } else if (alignment == TextAlignment.RIGHT) {
                x = Math.max(0, width - textWidth);
            } else {
                x = 0;
            }
            graphics.drawString(line, x, y);
            y += lineHeightPx;
        }

        graphics.dispose();
        return image;
    }

    private int fontStyle() {
        int style = Font.PLAIN;
        if (bold) {
            style |= Font.BOLD;
        }
        if (italic) {
            style |= Font.ITALIC;
        }
        return style;
    }
}
