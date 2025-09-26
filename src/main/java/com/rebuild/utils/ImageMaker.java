package com.rebuild.utils;

import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2023/1/8
 */
@Slf4j
public class ImageMaker {

    // 颜色
    public static final Color[] RB_COLORS = new Color[]{
            new Color(66, 133, 244),
            new Color(52, 168, 83),
            new Color(251, 188, 5),
            new Color(234, 67, 53),
            new Color(155, 82, 222),
            new Color(22, 168, 143),
    };

    /**
     * 生成头像
     *
     * @param name
     * @param dest
     */
    public static void makeAvatar(String name, File dest) {
        if (name.length() > 2) name = name.substring(name.length() - 2);
        name = name.toUpperCase();

        BufferedImage bi = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();

        g2d.setColor(RB_COLORS[RandomUtils.nextInt(RB_COLORS.length)]);
        g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        FileUtils.deleteQuietly(dest);
        try {
            final Font font = createFont(81f);
            g2d.setFont(font);
            g2d.setColor(Color.WHITE);
            FontMetrics fontMetrics = g2d.getFontMetrics(font);
            int x = fontMetrics.stringWidth(name);
            g2d.drawString(name, (200 - x) / 2, 128);
            g2d.setColor(new Color(0, 0, 0, 1));
            g2d.drawString("wbr", 0, 62);
            g2d.dispose();

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                ImageIO.write(bi, "png", fos);
                fos.flush();
            }

        } catch (Throwable ex) {
            log.warn("Cannot make font-avatar : {}", name, ex);

            InputStream is = null;
            try {
                is = CommonsUtils.getStreamOfRes("/web" + UserHelper.DEFAULT_AVATAR);
                bi = ImageIO.read(is);
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    ImageIO.write(bi, "png", fos);
                    fos.flush();
                }

            } catch (IOException ignored) {
                IOUtils.closeQuietly(is);
            }
        }
    }

    /**
     * 获取默认字体
     *
     * @param size
     * @return
     */
    public static Font createFont(float size) {
        File fontFile = RebuildConfiguration.getFileOfData("SourceHanSansK-Regular.ttf");
        if (fontFile.exists()) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                font = font.deriveFont(size);
                return font;
            } catch (Throwable ex) {
                log.warn("Cannot create Font: SourceHanSansK-Regular.ttf", ex);
            }
        }
        // Use default
        return new Font(Font.SERIF, Font.BOLD, (int) size);
    }

    /**
     * 添加水印
     *
     * @param image
     * @param text
     * @param dest
     * @throws IOException
     */
    public static void makeWatermark(File image, String text, File dest) throws IOException {
        BufferedImage inputImage = ImageIO.read(image);
        int iw = inputImage.getWidth();
        int iwm = Math.min(Math.max((int) (iw * 0.8), 100), 600);
        BufferedImage w = createTextWatermark(text, createFont(32f), Color.WHITE, iwm);

        Thumbnails.of(image)
                .scale(1.0)
                .watermark(Positions.BOTTOM_RIGHT, w, 0.6f)
                .outputQuality(1.0)
                .toFile(dest);
    }

    /**
     * 生成水印图
     *
     * @param text
     * @param font
     * @param textColor
     * @param maxWidth
     * @return
     */
    @SuppressWarnings({"SameParameterValue", "UnnecessaryLocalVariable"})
    static BufferedImage createTextWatermark(String text, Font font, Color textColor, int maxWidth) {
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImg.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        List<String> lines = new ArrayList<>();

        // 先按 \n 手动换行，再对每行自动折行
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (char c : paragraph.toCharArray()) {
                line.append(c);
                if (fm.stringWidth(line.toString()) > maxWidth) {
                    // 超出最大宽度，回退一个字符作为新行
                    line.deleteCharAt(line.length() - 1);
                    lines.add(line.toString());
                    line = new StringBuilder().append(c);
                }
            }
            if (line.length() > 1) lines.add(line.toString());
        }
        g2d.dispose();

        int lineHeight = fm.getHeight();
        int imageHeight = lines.size() * lineHeight;
        int imageWidth = maxWidth;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setFont(font);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int y = fm.getAscent();
        for (String l : lines) {
            // 阴影层
            g.setColor(new Color(0, 0, 0, 150)); // 半透明黑色阴影
            g.drawString(l, 1, y + 1);

            // 正文字体
            g.setColor(textColor);
            g.drawString(l, 0, y);

            y += lineHeight;
        }

        g.dispose();
        return image;
    }
}
