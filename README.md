# psd4j

Java 版 PSD 生成库，参考 [ag-psd](https://github.com/Agamnentzar/ag-psd) 的写出逻辑实现，无第三方运行时依赖。

## 功能

- 生成 8 位 RGB PSD 文件（PSD version 1）
- 添加图片图层（`BufferedImage`）
- 添加可编辑文本图层（TySh + EngineData），Photoshop 中可以继续修改文字
- 图层名称（含中文，写入 `luni`）、位置、透明度、可见性、混合模式
- 未指定合成图时，自动按可见图层合成底图

## 构建

```bash
mvn package
```

## 快速开始

```java
import com.xulihang.psd.Psd;
import com.xulihang.psd.TextAlignment;
import com.xulihang.psd.TextLayer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

Psd psd = new Psd(800, 600);

// backgroundImage 的类型是 java.awt.image.BufferedImage
// （可以用 ImageIO.read(new File("background.png")) 读取，也可以用 Graphics2D 自行绘制）
// 图片的宽高就是图层尺寸，left/top 是图层在文档中的左上角坐标
BufferedImage backgroundImage = ImageIO.read(new File("background.png"));
psd.addImageLayer("Background", backgroundImage, 0, 0);

TextLayer title = new TextLayer("Title", "Hello PSD\n你好，Photoshop！", 100, 200)
        .setFontFamily("Microsoft YaHei")  // Java2D 渲染用的字体
        .setFontName("MicrosoftYaHei")     // 写入 PSD 的字体名
        .setFontSize(56)
        .setBold(true)
        .setColor(new Color(0x1f2937))
        .setBox(600, 220)                  // 文本框大小；不设置则按文字自动测量
        .setAlignment(TextAlignment.LEFT)
        .setLineHeight(72);

psd.addTextLayer(title);
psd.write(Paths.get("output.psd"));
```

图片层接受任意 `java.awt.image.BufferedImage`；使用带透明通道的 `TYPE_INT_ARGB`
时，透明度会写入 PSD 图层的 alpha 通道。

也可以直接拿到字节数组：

```java
byte[] data = psd.toByteArray();
```

### API 一览

| 类 | 说明 |
| --- | --- |
| `Psd` | 文档：尺寸、图层列表、`toByteArray()` / `write(...)` |
| `Layer` | 图层基类：名称、位置、透明度、可见性、混合模式 |
| `ImageLayer` | 图片图层，内容为 `BufferedImage` |
| `TextLayer` | 文本图层：文字、字体、字号、颜色、对齐、文本框、行高、抗锯齿 |
| `TextAlignment` | 左对齐 / 居中 / 右对齐 |
| `BlendMode` | 常用混合模式（normal、multiply、screen、overlay 等） |

## 运行示例

```bash
mvn compile
java -cp target/classes com.xulihang.Main
```

会在当前目录生成 `output.psd`（渐变背景 + 半透明图片层 + 两个文本层）。

## 说明与限制

- 只支持写出，不支持读取 PSD。
- 仅支持 8 位 RGB 色彩模式。
- 不支持图层组、图层蒙版、智能对象、CMYK、16/32 位文档。
- 文本图层的像素数据由 Java2D 渲染，仅作为预览；Photoshop 会依据 TySh 中的文本数据重新排版，
  字体缺失或自动换行时预览与最终效果可能略有差异。
- 合成图按普通叠加（normal）绘制，非 normal 混合模式只在 PSD 图层属性中生效。
