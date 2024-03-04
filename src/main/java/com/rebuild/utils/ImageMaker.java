package com.rebuild.utils;

import com.rebuild.core.RebuildException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.RebuildConfiguration;
import lombok.extern.slf4j.Slf4j;
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
     * 生成LOGO（效果不佳暂不用）
     *
     * @param text
     * @param color
     * @param dest
     */
    @Deprecated
    public static void makeLogo(String text, Color color, File dest) {
        BufferedImage bi = new BufferedImage(300, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color textColor = color == null ? RB_COLORS[RandomUtils.nextInt(RB_COLORS.length)] : color;

        FileUtils.deleteQuietly(dest);
        try {
            final Font font = createFont(63f);
            g2d.setFont(font);
            g2d.setColor(textColor);
            FontMetrics fontMetrics = g2d.getFontMetrics(font);
            int x = fontMetrics.stringWidth(text);
            g2d.drawString(text, (300 - x) / 2, 60 - 6);

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                ImageIO.write(bi, "png", fos);
                fos.flush();
            }

        } catch (Throwable ex) {
            throw new RebuildException("Cannot make logo", ex);
        }
    }

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
     * 获取字体
     *
     * @return
     */
    static Font createFont(float size) {
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
}
