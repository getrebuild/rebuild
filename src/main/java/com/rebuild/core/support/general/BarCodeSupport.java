/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.general;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.RebuildConfiguration;
import org.apache.commons.lang.StringUtils;

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
public class BarCodeSupport {

    // 二维码（默认）
    public static final String TYPE_QRCODE = "QRCODE";
    // 条码
    public static final String TYPE_BARCODE = "BARCODE";

    /**
     * @param field
     * @param record
     * @return
     */
    public static String getBarCodeContent(Field field, ID record) {
        String barcodeFormat = EasyMetaFactory.valueOf(field).getExtraAttr("barcodeFormat");
        if (StringUtils.isBlank(barcodeFormat)) {
            return "UNSET";
        }
        return ContentWithFieldVars.replaceWithRecord(barcodeFormat, record);
    }

    /**
     * @param field
     * @param record
     * @return
     */
    public static BufferedImage getBarCodeImage(Field field, ID record) {
        String content = getBarCodeContent(field, record);
        String barcodeType = EasyMetaFactory.valueOf(field).getExtraAttr("barcodeType");

        if (TYPE_BARCODE.equalsIgnoreCase(barcodeType)) {
            return createBarCode(content);
        }
        // 默认为二维码
        else {
            return createQRCode(content);
        }
    }

    /**
     * QR_CODE
     *
     * @param content
     * @return
     */
    public static BufferedImage createQRCode(String content) {
        BitMatrix bitMatrix = createCode(content, BarcodeFormat.QR_CODE, 320);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * CODE_128
     *
     * @param content
     * @return
     */
    public static BufferedImage createBarCode(String content) {
        BitMatrix bitMatrix = createCode(content, BarcodeFormat.CODE_128, 320);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * @param content
     * @param format
     * @param height
     * @return
     */
    public static BitMatrix createCode(String content, BarcodeFormat format, int height) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);

        // 条形码宽度为自适应
        int width = format == BarcodeFormat.QR_CODE ? height : 0;
        try {
            return new MultiFormatWriter().encode(content, format, width, height, hints);

        } catch (WriterException ex) {
            throw new RebuildException("Encode BarCode error : " + content, ex);
        }
    }

    /**
     * @param content
     * @param format
     * @param height
     * @return
     */
    public static File saveCode(String content, BarcodeFormat format, int height) {
        BitMatrix bitMatrix = createCode(content, format, height);

        String fileName = String.format("BarCode-%d.png", System.currentTimeMillis());
        File dest = RebuildConfiguration.getFileOfTemp(fileName);
        try {
            MatrixToImageWriter.writeToPath(bitMatrix, "png", dest.toPath());
            return dest;

        } catch (IOException ex) {
            throw new RebuildException("Write BarCode error : " + content, ex);
        }
    }
}
