package com.xulihang.psd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the Photoshop text engine data blob (the binary "EngineData" field of
 * a TySh record). The structure follows what ag-psd writes, which is the
 * minimal description Photoshop needs to keep a text layer editable.
 */
final class EngineDataWriter {
    private static final List<String> FLOAT_KEYS = Arrays.asList(
            "Axis", "XY", "Zone", "WordSpacing", "FirstLineIndent", "GlyphSpacing", "StartIndent",
            "EndIndent", "SpaceBefore", "SpaceAfter", "LetterSpacing", "Values", "GridSize",
            "GridLeading", "PointBase", "BoxBounds", "TransformPoint0", "TransformPoint1",
            "TransformPoint2", "FontSize", "Leading", "HorizontalScale", "VerticalScale",
            "BaselineShift", "Tsume", "OutlineWidth", "AutoLeading");

    private static final String KINSOKU_HARD_NOSTART =
            "、。，．・：；？！ー―’”）〕］｝〉》」』】ヽヾゝゞ々ぁぃぅぇぉっゃゅょゎァィゥェォッャュョヮヵヶ゛゜?!)]},.:;℃℉¢％‰";
    private static final String KINSOKU_HARD_NOEND =
            "‘“（〔［｛〈《「『【([{￥＄£＠§〒＃";
    private static final String KINSOKU_SOFT_NOSTART =
            "、。，．・：；？！’”）〕］｝〉》」』】ヽヾゝゞ々";
    private static final String KINSOKU_SOFT_NOEND =
            "‘“（〔［｛〈《「『【";

    private final BinaryWriter out = new BinaryWriter();
    private int indent;

    private EngineDataWriter() {
    }

    static byte[] write(TextLayer layer, int width, int height) {
        return new EngineDataWriter().serialize(build(layer, width, height));
    }

    private static Map<String, Object> build(TextLayer layer, int width, int height) {
        String text = toEngineText(layer.getText()) + "\r";
        int fontIndex = 1; // index 0 is AdobeInvisFont

        Map<String, Object> paragraph = paragraphProperties(layer.getAlignment(), layer.getOrientation().isVertical());
        Map<String, Object> fullStyle = fullStyle(layer, fontIndex);
        Map<String, Object> runStyle = runStyle(layer, fontIndex);

        Map<String, Object> editor = map("Text", text);

        List<Object> paragraphRuns = new ArrayList<>();
        List<Object> paragraphRunLengths = new ArrayList<>();
        int last = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\r') {
                paragraphRunLengths.add(i - last + 1);
                paragraphRuns.add(map(
                        "ParagraphSheet", map(
                                "DefaultStyleSheet", 0,
                                "Properties", paragraph),
                        "Adjustments", map(
                                "Axis", numbers(1, 0, 1),
                                "XY", numbers(0, 0))));
                last = i + 1;
            }
        }
        if (last < text.length()) {
            paragraphRunLengths.add(text.length() - last);
            paragraphRuns.add(map(
                    "ParagraphSheet", map(
                            "DefaultStyleSheet", 0,
                            "Properties", paragraph),
                    "Adjustments", map(
                            "Axis", numbers(1, 0, 1),
                            "XY", numbers(0, 0))));
        }

        List<Object> styleRuns = new ArrayList<>();
        styleRuns.add(map(
                "StyleSheet", map(
                        "StyleSheetData", runStyle)));

        Map<String, Object> photoshop = map(
                "ShapeType", 1,
                "BoxBounds", numbers(0, 0, width, height),
                "Base", map(
                        "ShapeType", 1,
                        "TransformPoint0", numbers(1, 0),
                        "TransformPoint1", numbers(0, 1),
                        "TransformPoint2", numbers(0, 0)));

        int writingDirection = layer.getOrientation().getWritingDirection();

        Map<String, Object> textShape = map(
                "ShapeType", 1,
                "Procession", layer.getOrientation().getProcession(),
                "Lines", map(
                        "WritingDirection", writingDirection,
                        "Children", list()),
                "Cookie", map("Photoshop", photoshop));

        Map<String, Object> rendered = map(
                "Version", 1,
                "Shapes", map(
                        "WritingDirection", writingDirection,
                        "Children", list(textShape)));

        Map<String, Object> engineDict = map(
                "Editor", editor,
                "ParagraphRun", map(
                        "DefaultRunData", map(
                                "ParagraphSheet", map(
                                        "DefaultStyleSheet", 0,
                                        "Properties", map()),
                                "Adjustments", map(
                                        "Axis", numbers(1, 0, 1),
                                        "XY", numbers(0, 0))),
                        "RunArray", paragraphRuns,
                        "RunLengthArray", paragraphRunLengths,
                        "IsJoinable", 1),
                "StyleRun", map(
                        "DefaultRunData", map(
                                "StyleSheet", map(
                                        "StyleSheetData", map())),
                        "RunArray", styleRuns,
                        "RunLengthArray", numbers(text.length()),
                        "IsJoinable", 2),
                "GridInfo", map(
                        "GridIsOn", false,
                        "ShowGrid", false,
                        "GridSize", 18.0,
                        "GridLeading", 22.0,
                        "GridColor", color(0, 0, 255),
                        "GridLeadingFillColor", color(0, 0, 255),
                        "AlignLineHeightToGridFlags", false),
                "AntiAlias", antiAliasIndex(layer.getAntiAlias()),
                "UseFractionalGlyphWidths", true,
                "Rendered", rendered);

        Map<String, Object> resources = resourceDict(layer, fontIndex, paragraph, fullStyle);

        return map(
                "EngineDict", engineDict,
                "ResourceDict", resources,
                "DocumentResources", resources);
    }

    private static Map<String, Object> resourceDict(
            TextLayer layer, int fontIndex, Map<String, Object> paragraph, Map<String, Object> style) {
        List<Object> kinsoku = list(
                map(
                        "Name", "PhotoshopKinsokuHard",
                        "NoStart", KINSOKU_HARD_NOSTART,
                        "NoEnd", KINSOKU_HARD_NOEND,
                        "Keep", "―‥",
                        "Hanging", "、。.,"),
                map(
                        "Name", "PhotoshopKinsokuSoft",
                        "NoStart", KINSOKU_SOFT_NOSTART,
                        "NoEnd", KINSOKU_SOFT_NOEND,
                        "Keep", "―‥",
                        "Hanging", "、。.,"));

        List<Object> mojikumi = list(
                map("InternalName", "Photoshop6MojiKumiSet1"),
                map("InternalName", "Photoshop6MojiKumiSet2"),
                map("InternalName", "Photoshop6MojiKumiSet3"),
                map("InternalName", "Photoshop6MojiKumiSet4"));

        List<Object> fonts = list(
                map("Name", "AdobeInvisFont", "Script", 0, "FontType", 0, "Synthetic", 0),
                map("Name", layer.getFontName(), "Script", 0, "FontType", 0, "Synthetic", 0));

        return map(
                "KinsokuSet", kinsoku,
                "MojiKumiSet", mojikumi,
                "TheNormalStyleSheet", 0,
                "TheNormalParagraphSheet", 0,
                "ParagraphSheetSet", list(map(
                        "Name", "Normal RGB",
                        "DefaultStyleSheet", 0,
                        "Properties", paragraph)),
                "StyleSheetSet", list(map(
                        "Name", "Normal RGB",
                        "StyleSheetData", style)),
                "FontSet", fonts,
                "SuperscriptSize", 0.583,
                "SuperscriptPosition", 0.333,
                "SubscriptSize", 0.583,
                "SubscriptPosition", 0.333,
                "SmallCapSize", 0.7);
    }

    private static Map<String, Object> paragraphProperties(TextAlignment alignment, boolean vertical) {
        return map(
                "Justification", alignment.getJustification(),
                "FirstLineIndent", 0.0,
                "StartIndent", 0.0,
                "EndIndent", 0.0,
                "SpaceBefore", 0.0,
                "SpaceAfter", 0.0,
                "AutoHyphenate", true,
                "HyphenatedWordSize", 6,
                "PreHyphen", 2,
                "PostHyphen", 2,
                "ConsecutiveHyphens", 8,
                "Zone", 36.0,
                "WordSpacing", numbers(0.8, 1, 1.33),
                "LetterSpacing", numbers(0, 0, 0),
                "GlyphSpacing", numbers(1, 1, 1),
                "AutoLeading", 1.2,
                "LeadingType", 0,
                "Hanging", false,
                "Burasagari", false,
                "KinsokuOrder", vertical ? 1 : 0,
                "EveryLineComposer", false);
    }

    private static Map<String, Object> fullStyle(TextLayer layer, int fontIndex) {
        return map(
                "Font", fontIndex,
                "FontSize", layer.getFontSize(),
                "FauxBold", layer.isBold(),
                "FauxItalic", layer.isItalic(),
                "AutoLeading", true,
                "Leading", 0.0,
                "HorizontalScale", 1.0,
                "VerticalScale", 1.0,
                "Tracking", 0,
                "AutoKerning", true,
                "Kerning", 0,
                "BaselineShift", 0.0,
                "FontCaps", 0,
                "FontBaseline", 0,
                "Underline", false,
                "Strikethrough", false,
                "Ligatures", true,
                "DLigatures", false,
                "BaselineDirection", 2,
                "Tsume", 0.0,
                "StyleRunAlignment", 2,
                "Language", 0,
                "NoBreak", false,
                "FillColor", color(layer.getColor().getRed(), layer.getColor().getGreen(),
                        layer.getColor().getBlue()),
                "StrokeColor", color(0, 0, 0),
                "FillFlag", true,
                "StrokeFlag", false,
                "FillFirst", true,
                "YUnderline", 1,
                "OutlineWidth", 1.0,
                "CharacterDirection", layer.getOrientation().isVertical() ? 1 : 0,
                "HindiNumbers", false,
                "Kashida", 1,
                "DiacriticPos", 2);
    }

    private static Map<String, Object> runStyle(TextLayer layer, int fontIndex) {
        Map<String, Object> style = map(
                "Font", fontIndex,
                "FontSize", layer.getFontSize(),
                "FauxBold", layer.isBold());
        if (layer.isItalic()) {
            style.put("FauxItalic", true);
        }
        style.put("AutoKerning", true);
        style.put("Kerning", 0);
        style.put("FillColor", color(layer.getColor().getRed(), layer.getColor().getGreen(),
                layer.getColor().getBlue()));
        return style;
    }

    static String toEngineText(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n').replace('\n', '\r');
    }

    static int antiAliasIndex(String antiAlias) {
        switch (antiAlias == null ? "sharp" : antiAlias) {
            case "none": return 0;
            case "crisp": return 1;
            case "strong": return 2;
            case "smooth": return 3;
            case "platformLCD": return 6;
            case "platform": return 7;
            case "sharp":
            default: return 4;
        }
    }

    static String antiAliasEnum(String antiAlias) {
        switch (antiAlias == null ? "sharp" : antiAlias) {
            case "none": return "Anno";
            case "crisp": return "AnCr";
            case "strong": return "AnSt";
            case "smooth": return "AnSm";
            case "platformLCD": return "antiAliasPlatformLCD";
            case "platform": return "antiAliasPlatformGray";
            case "sharp":
            default: return "antiAliasSharp";
        }
    }

    private static Map<String, Object> color(int r, int g, int b) {
        return map(
                "Type", 1,
                "Values", numbers(1, r / 255.0, g / 255.0, b / 255.0));
    }

    private static Map<String, Object> map(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put((String) values[i], values[i + 1]);
        }
        return map;
    }

    private static List<Object> list(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static List<Double> numbers(double... values) {
        List<Double> list = new ArrayList<>(values.length);
        for (double value : values) {
            list.add(value);
        }
        return list;
    }

    private byte[] serialize(Object root) {
        out.writeAscii("\n\n");
        writeValue(root, null, false);
        return out.toByteArray();
    }

    private void writeValue(Object value, String key, boolean inProperty) {
        if (value == null) {
            writePrefix(inProperty);
            out.writeAscii("null");
        } else if (value instanceof Number) {
            writePrefix(inProperty);
            writeNumber(((Number) value).doubleValue(), key);
        } else if (value instanceof Boolean) {
            writePrefix(inProperty);
            out.writeAscii(((Boolean) value) ? "true" : "false");
        } else if (value instanceof String) {
            writePrefix(inProperty);
            writeText((String) value);
        } else if (value instanceof List) {
            writeArray((List<?>) value, key, inProperty);
        } else if (value instanceof Map) {
            writeObject((Map<?, ?>) value, inProperty);
        } else {
            throw new IllegalArgumentException("Unsupported engine data value: " + value);
        }
    }

    private void writeArray(List<?> values, String key, boolean inProperty) {
        writePrefix(inProperty);
        out.writeByte('[');

        boolean allNumbers = true;
        for (Object value : values) {
            if (!(value instanceof Number)) {
                allNumbers = false;
                break;
            }
        }

        if (allNumbers) {
            boolean intArray = "RunLengthArray".equals(key);
            for (Object value : values) {
                out.writeByte(' ');
                if (intArray) {
                    out.writeAscii(Long.toString((long) ((Number) value).doubleValue()));
                } else {
                    writeFloat(((Number) value).doubleValue());
                }
            }
            out.writeAscii(" ]");
        } else {
            out.writeByte('\n');
            for (Object value : values) {
                writeValue(value, key, false);
                out.writeByte('\n');
            }
            writeIndent();
            out.writeByte(']');
        }
    }

    private void writeObject(Map<?, ?> values, boolean inProperty) {
        if (inProperty) {
            out.writeByte('\n');
        }
        writeIndent();
        out.writeAscii("<<");
        out.writeByte('\n');
        indent++;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String key = (String) entry.getKey();
            writeIndent();
            out.writeByte('/');
            out.writeAscii(key);
            writeValue(value, key, true);
            out.writeByte('\n');
        }
        indent--;
        writeIndent();
        out.writeAscii(">>");
    }

    private void writePrefix(boolean inProperty) {
        if (inProperty) {
            out.writeByte(' ');
        } else {
            writeIndent();
        }
    }

    private void writeIndent() {
        for (int i = 0; i < indent; i++) {
            out.writeByte('\t');
        }
    }

    private void writeText(String text) {
        out.writeByte('(');
        out.writeByte(0xfe);
        out.writeByte(0xff);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            writeTextByte((c >>> 8) & 0xff);
            writeTextByte(c & 0xff);
        }
        out.writeByte(')');
    }

    private void writeTextByte(int value) {
        if (value == '(' || value == ')' || value == '\\') {
            out.writeByte('\\');
        }
        out.writeByte(value);
    }

    private void writeNumber(double value, String key) {
        boolean isFloat = (key != null && FLOAT_KEYS.contains(key)) || value != Math.floor(value);
        if (isFloat) {
            writeFloat(value);
        } else {
            out.writeAscii(Long.toString((long) value));
        }
    }

    private void writeFloat(double value) {
        if (!Double.isFinite(value)) {
            value = 0;
        }
        String text = String.format(Locale.ROOT, "%.5f", value)
                .replaceAll("(\\d)0+$", "$1")
                .replaceAll("^0+\\.([1-9])", ".$1")
                .replaceAll("^-0+\\.0(\\d)", "-.0$1");
        out.writeAscii(text);
    }
}
