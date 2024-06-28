/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.CalendarUtils;
import cn.hutool.core.convert.Convert;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * @author devezhao
 * @since 2024/6/25
 */
@Slf4j
public class ValueConvertFunc {

    // # 函数
    protected static final String FUNC_SPLITER = "#";

    /**
     * @param field
     * @param value
     * @param varName
     * @return
     */
    public static Object convert(EasyField field, Object value, String varName) {
        String thatFunc = splitFunc(varName);
        if (thatFunc == null) return value;

        final DisplayType type = field.getDisplayType();
        if (type == DisplayType.NUMBER || type == DisplayType.DECIMAL) {
            if ("CHINESEYUAN".equals(thatFunc)) {
                return Convert.digitToChinese((Number) value);
            }
            if ("THOUSANDS".equals(thatFunc)) {
                String format = "##,##0";
                if (type == DisplayType.DECIMAL) {
                    int scale = ((EasyDecimal) field).getScale();
                    if (scale > 0) {
                        format += "." + StringUtils.leftPad("", scale, "0");
                    }
                }
                return new DecimalFormat(format).format(value);
            }

        } else if (type == DisplayType.DATE || type == DisplayType.DATETIME) {
            if ("CHINESEDATE".equals(thatFunc)) {
                Date d = CommonsUtils.parseDate(value.toString());
                if (d == null) return value;

                int len = field.wrapValue(CalendarUtils.now()).toString().length();
                if (len <= 10) len += 1;  // yyyy-MM-dd
                else len += 2;

                String format = CalendarUtils.CN_DATETIME_FORMAT.substring(0, len);
                return CalendarUtils.getDateFormat(format).format(d);
            }

        }
        return value;
    }

    /**
     * 转换图片
     *
     * @param value
     * @param varName
     * @return
     */
    public static PictureRenderData convertPictureWithSize(byte[] value, String varName) {
        Pictures.PictureBuilder builder = Pictures.ofBytes(value);

        String thatFunc = splitFunc(varName);
        if (thatFunc == null) return builder.create();

        if (thatFunc.startsWith("SIZE") && thatFunc.length() > 4) {
            String[] wh = thatFunc.substring(4).split("\\*");
            int width = NumberUtils.toInt(wh[0]);
            int height = -1;

            // 指定宽度，高度自适应
            if (wh.length == 1) {
                try (InputStream is = new ByteArrayInputStream(value)) {
                    BufferedImage bi = ImageIO.read(is);
                    int originWidth = bi.getWidth();
                    int originHeight = bi.getHeight();
                    if (originWidth > 0 && originHeight > 0) {
                        double scale = width * 1.0 / originWidth;
                        height = (int) (originHeight * scale);
                    }

                } catch (IOException e) {
                    log.error(null, e);
                }
            } else {
                height = NumberUtils.toInt(wh[1]);
            }

            if (height < 0) height = width;
            builder = Pictures.ofBytes(value).size(width, height);
        }

        return builder.create();
    }

    /**
     * @param varName
     * @return
     */
    public static String splitName(String varName) {
        return varName.split("#")[0].trim();
    }

    /**
     * @param varName
     * @return
     */
    public static String splitFunc(String varName) {
        return varName.contains(FUNC_SPLITER) ? varName.split("#")[1].trim() : null;
    }
}
