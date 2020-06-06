/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.helper.fieldvalue;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.rebuild.server.RebuildException;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码字段
 *
 * @author devezhao
 * @since 2020/6/5
 */
public class BarCodeGenerator {

    /**
     * @param field
     * @param record
     * @return
     */
    public static String getBarCodeContent(Field field, ID record) {
        String barcodeFormat = EasyMeta.valueOf(field).getExtraAttr("barcodeFormat");
        if (StringUtils.isBlank(barcodeFormat)) {
            return "UNSET";
        }
        return ContentWithFieldVars.replace(barcodeFormat, record);
    }

    /**
     * @param field
     * @param record
     * @return
     */
    public static File getBarCodeImage(Field field, ID record) {
        String content = getBarCodeContent(field, record);
        String barcodeType = EasyMeta.valueOf(field).getExtraAttr("barcodeType");

        if ("QRCODE".equalsIgnoreCase(barcodeType)) {
            return createQRCode(content);
        } else {
            return createBarCode(content);
        }
    }

    /**
     * QR_CODE
     *
     * @param content
     * @return
     */
    public static File createQRCode(String content) {
        return createBarCode(content, BarcodeFormat.QR_CODE, 240, 240);
    }

    /**
     * CODE_128
     *
     * @param content
     * @return
     */
    public static File createBarCode(String content) {
        // 条形码宽度为自适应
        return createBarCode(content, BarcodeFormat.CODE_128, 12, 120);
    }

    /**
     * @param content
     * @param format
     * @param width
     * @param height
     * @return
     */
    public static File createBarCode(String content, BarcodeFormat format, int width, int height) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, format, width, height, hints);

            String fileName = String.format("BarCode-%d.png", System.currentTimeMillis());
            File dest = SysConfiguration.getFileOfTemp(fileName);
            MatrixToImageWriter.writeToPath(bitMatrix, "png", dest.toPath());
            return dest;

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (WriterException | IOException ex) {
            throw new RebuildException("Write BarCode failed : " + content, ex);
        }
    }
}
