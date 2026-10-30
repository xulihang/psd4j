package com.xulihang.psd;

/**
 * Writes the TySh (type tool object setting) additional layer information that
 * makes a text layer editable in Photoshop.
 */
final class TyShWriter {
    private TyShWriter() {
    }

    static byte[] write(TextLayer layer, int width, int height) {
        byte[] engineData = EngineDataWriter.write(layer, width, height);

        BinaryWriter out = new BinaryWriter();
        out.writeI16(1); // version

        double left = layer.getLeft();
        double top = layer.getTop();
        out.writeF64(1); // transform: a, b, c, d, tx, ty
        out.writeF64(0);
        out.writeF64(0);
        out.writeF64(1);
        out.writeF64(left);
        out.writeF64(top);

        out.writeI16(50); // text version
        out.writeU32(16); // descriptor version
        writeTextDescriptor(out, layer, engineData);

        out.writeI16(1); // warp version
        out.writeU32(16); // descriptor version
        writeWarpDescriptor(out);

        out.writeF32((float) left);
        out.writeF32((float) top);
        out.writeF32((float) (left + width));
        out.writeF32((float) (top + height));
        return out.toByteArray();
    }

    private static void writeTextDescriptor(BinaryWriter out, TextLayer layer, byte[] engineData) {
        out.writeUnicodeStringWithPadding("");
        out.writeAsciiStringOrClassId("TxLr");
        out.writeU32(6); // number of fields

        out.writeAsciiStringOrClassId("Txt ");
        out.writeSignature("TEXT");
        out.writeUnicodeStringWithPadding(EngineDataWriter.toEngineText(layer.getText()));

        out.writeAsciiStringOrClassId("textGridding");
        out.writeSignature("enum");
        writeEnum(out, "textGridding", "None");

        out.writeAsciiStringOrClassId("Ornt");
        out.writeSignature("enum");
        writeEnum(out, "Ornt", layer.getOrientation().getEnumValue());

        out.writeAsciiStringOrClassId("AntA");
        out.writeSignature("enum");
        writeEnum(out, "Annt", EngineDataWriter.antiAliasEnum(layer.getAntiAlias()));

        out.writeAsciiStringOrClassId("TextIndex");
        out.writeSignature("long");
        out.writeI32(0);

        out.writeAsciiStringOrClassId("EngineData");
        out.writeSignature("tdta");
        out.writeU32(engineData.length);
        out.writeBytes(engineData);
    }

    private static void writeWarpDescriptor(BinaryWriter out) {
        out.writeUnicodeStringWithPadding("");
        out.writeAsciiStringOrClassId("warp");
        out.writeU32(8); // number of fields

        out.writeAsciiStringOrClassId("warpStyle");
        out.writeSignature("enum");
        writeEnum(out, "warpStyle", "warpNone");

        out.writeAsciiStringOrClassId("warpValue");
        out.writeSignature("doub");
        out.writeF64(0);

        out.writeAsciiStringOrClassId("warpPerspective");
        out.writeSignature("doub");
        out.writeF64(0);

        out.writeAsciiStringOrClassId("warpPerspectiveOther");
        out.writeSignature("doub");
        out.writeF64(0);

        out.writeAsciiStringOrClassId("warpRotate");
        out.writeSignature("enum");
        writeEnum(out, "Ornt", "Hrzn");

        out.writeAsciiStringOrClassId("bounds");
        out.writeSignature("Objc");
        writeWarpBounds(out);

        out.writeAsciiStringOrClassId("uOrder");
        out.writeSignature("long");
        out.writeI32(0);

        out.writeAsciiStringOrClassId("vOrder");
        out.writeSignature("long");
        out.writeI32(0);
    }

    private static void writeWarpBounds(BinaryWriter out) {
        // empty class name + class "bounds"
        out.writeUnicodeStringWithPadding("");
        out.writeAsciiStringOrClassId("bounds");
        out.writeU32(4);

        writePixelUnit(out, "Left");
        writePixelUnit(out, "Top ");
        writePixelUnit(out, "Rght");
        writePixelUnit(out, "Btom");
    }

    private static void writePixelUnit(BinaryWriter out, String key) {
        out.writeAsciiStringOrClassId(key);
        out.writeSignature("UntF");
        out.writeSignature("#Pxl");
        out.writeF64(0);
    }

    private static void writeEnum(BinaryWriter out, String type, String value) {
        out.writeAsciiStringOrClassId(type);
        out.writeAsciiStringOrClassId(value);
    }
}
