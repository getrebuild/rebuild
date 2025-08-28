/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码字段支持
 *
 * @author devezhao
 * @since 2020/6/5
 */
@Slf4j
public class BarCodeSupport {

    // 二维码（默认）
    public static final String TYPE_QRCODE = "QRCODE";
    // 条码
    public static final String TYPE_BARCODE = "BARCODE";

    private static final String CONTENT_UNSET = "UNSET";
    private static final String CONTENT_ERROR = "ERROR";

    /**
     * @param field
     * @param record
     * @return
     */
    public static String getBarCodeContent(Field field, ID record) {
        String barcodeFormat = EasyMetaFactory.valueOf(field).getExtraAttr("barcodeFormat");
        if (StringUtils.isBlank(barcodeFormat)) return CONTENT_UNSET;
        return ContentWithFieldVars.replaceWithRecord(barcodeFormat, record);
    }

    /**
     * @param field
     * @param record
     * @return
     */
    public static BufferedImage getBarCodeImage(Field field, ID record) {
        String content = getBarCodeContent(field, record);
        if (StringUtils.isBlank(content)) return null;

        EasyField easyField = EasyMetaFactory.valueOf(field);
        String barcodeType = easyField.getExtraAttr(EasyFieldConfigProps.BARCODE_TYPE);

        if (TYPE_BARCODE.equalsIgnoreCase(barcodeType)) {
            return createBarCode(content, 0, Boolean.TRUE);
        } else {
            // 默认为二维码
            return createQRCode(content, 0);
        }
    }

    /**
     * QR_CODE
     *
     * @param content
     * @param width
     * @return
     */
    public static BufferedImage createQRCode(String content, int width) {
        BitMatrix bitMatrix = createBarCodeImage(content, BarcodeFormat.QR_CODE, width, 0);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * CODE_128
     *
     * @param content
     * @param height
     * @param showText 显示底部文字
     * @return
     */
    public static BufferedImage createBarCode(String content, int height, boolean showText) {
        return createBarCode(content, 0, height, showText, BarcodeFormat.CODE_128);
    }

    /**
     * CODE_128
     *
     * @param content
     * @param width 通常无需指定，自适应
     * @param height
     * @param showText 显示底部文字
     * @return
     */
    public static BufferedImage createBarCode(String content, int width, int height, boolean showText, BarcodeFormat specFormat) {
        BitMatrix bitMatrix;
        try {
            bitMatrix = createBarCodeImage(content, specFormat, width, height);
        } catch (IllegalArgumentException ex) {
            log.error("Cannot encode `{}` to {}", content, specFormat);

            content = CONTENT_ERROR;
            bitMatrix = createBarCodeImage(content, BarcodeFormat.CODE_128, width, height);
        }

        BufferedImage bi = MatrixToImageWriter.toBufferedImage(bitMatrix);

        if (showText) {
            try {
                // height = 64+16
                return drawTextOnImage(content, bi, bi.getHeight() / 4);
            } catch (Exception ex) {
                log.warn("Cannot draw text on barcode : {}", content, ex);
            }
        }
        return bi;
    }

    /**
     * @param content
     * @param format
     * @param height
     * @return
     */
    protected static BitMatrix createBarCodeImage(String content, BarcodeFormat format, int width, int height) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, AppUtils.UTF8);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        if (width > 1200) width = 1200;
        if (height > 1200) height = 1200;

        try {
            if (format == BarcodeFormat.QR_CODE) {
                width = height = Math.max(width, height);
                if (width <= 0) width = height = 120;

            } else {
                final int base = 64;
                if (height < base) height = base;

                // 条形码宽度为自适应
                if (width == 0 && height > base) {
                    width = (int) (((height + 0d) / base * 1.51) * new Code128Writer().encode(content).length);
                }
            }

            return new MultiFormatWriter().encode(content, format, width, height, hints);
        } catch (WriterException ex) {
            throw new RebuildException("Encode BarCode error : " + content, ex);
        }
    }

    /**
     * 保存文件
     *
     * @param image
     * @return
     */
    public static File saveCode(BitMatrix image) {
        String fileName = String.format("BarCode-%d.png", System.currentTimeMillis());
        File dest = RebuildConfiguration.getFileOfTemp(fileName);

        try {
            MatrixToImageWriter.writeToPath(image, "png", dest.toPath());
            return dest;

        } catch (IOException ex) {
            throw new RebuildException("Write BarCode error", ex);
        }
    }

    // --
    // https://github.com/zxing/zxing/issues/1099

    private static BufferedImage drawTextOnImage(String text, BufferedImage image, int space) {
        BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight() + space, BufferedImage.TRANSLUCENT);
        Graphics2D g2d = bi.createGraphics();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

        g2d.drawImage(image, 0, 0, null);

        final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, space);
        final int w = bi.getWidth();
        final int h = space;

        final FontMetrics fm = feetFontSizeToRegion(text, font, g2d, w, h);
        final Rectangle2D stringBounds = fm.getStringBounds(text, g2d);

        final double x = (w - stringBounds.getWidth()) / 2d;
        final double y = (bi.getHeight() - space) + (h - stringBounds.getHeight()) / 2d;

        if (CONTENT_UNSET.equals(text) || CONTENT_ERROR.equals(text)) {
            g2d.setColor(Color.RED);
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.fillRect(0, image.getHeight(), w, h);

        // center text at bottom of image in the new space
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, (int) x, (int) (y + fm.getAscent()));
        g2d.dispose();
        return bi;
    }

    private static FontMetrics feetFontSizeToRegion(String text, Font font, Graphics2D g2d, int regionWidth, int regionHeight) {
        // Get the fonts metrics
        FontMetrics fm = g2d.getFontMetrics(font);

        // Calculate the scaling requirements
        float xScale = (float) ((double) regionWidth / fm.stringWidth(text));
        float yScale = (float) ((double) regionHeight / fm.getHeight());

        // Determine which access to scale on...
        float scale = Math.min(xScale, yScale);

        // Create a new font using the scaling facter
        g2d.setFont(font.deriveFont(AffineTransform.getScaleInstance(scale, scale)));
        // Make it pretty
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Get the "scaled" metrics
        fm = g2d.getFontMetrics();

        return fm;
    }

    /**
     * 识别。支持条码与二维码
     *
     * @param image
     * @return
     */
    public static String decode(File image) {
        try {
            BufferedImage bufferedImage = ImageIO.read(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
            MultiFormatReader reader = new MultiFormatReader();

            Result result = reader.decode(bitmap);
            bufferedImage.flush();
            return result.getText();

        } catch (Exception e) {
            log.error("Cannot decode image : {}", image);
            return null;
        }
    }
}
