/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.hutool.core.convert.Convert;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDecimal;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author devezhao
 * @since 2024/6/25
 */
@Slf4j
public class ValueConvertFunc {

    // # 函数分隔符
    private static final String FUNC_SPLITER = "#";
    private static final String FVAL_SPLITER = ":";
    // 支持的函数
    private static final String DECIMAL_CHINESEYUAN = "CHINESEYUAN";
    private static final String DECIMAL_THOUSANDS = "THOUSANDS";
    private static final String DATE_CHINESEDATE = "CHINESEDATE";
    private static final String OPTION_CHECKBOX = "CHECKBOX";
    private static final String OPTION_CHECKBOX2 = "CHECKBOX" + FVAL_SPLITER + "2";
    private static final String CLASS_PICK = "PICK";
    private static final String IMAGE_SIZE = "SIZE";
    private static final String ALL_EMPTY = "EMPTY";

    /**
     * @param field
     * @param value
     * @param varName
     * @return
     */
    public static Object convert(EasyField field, Object value, String varName) {
        String thatFunc = splitFunc(varName);
        if (thatFunc == null) return value;

        // 默认值
        if (thatFunc.startsWith(ALL_EMPTY)) {
            if (value == null || StringUtils.isBlank(value.toString())) {
                return extractFuncValue(thatFunc);
            }
        }

        final DisplayType type = field.getDisplayType();
        if (type == DisplayType.NUMBER || type == DisplayType.DECIMAL) {
            if (DECIMAL_CHINESEYUAN.equals(thatFunc)) {
                return Convert.digitToChinese((Number) value);
            }
            if (DECIMAL_THOUSANDS.equals(thatFunc)) {
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
            if (DATE_CHINESEDATE.equals(thatFunc)) {
                Date d = CommonsUtils.parseDate(value.toString());
                if (d == null) return value;

                int len = field.wrapValue(CalendarUtils.now()).toString().length();
                if (len <= 10) len += 1;  // yyyy-MM-dd
                else len += 2;

                String format = CalendarUtils.CN_DATETIME_FORMAT.substring(0, len);
                return CalendarUtils.getDateFormat(format).format(d);
            }

        } else if (type == DisplayType.MULTISELECT || type == DisplayType.PICKLIST) {
            if (OPTION_CHECKBOX.equals(thatFunc) || OPTION_CHECKBOX2.equals(thatFunc)) {
                String[] m = value == null ? new String[0] : value.toString().split(", ");
                ConfigBean[] items = MultiSelectManager.instance.getPickListRaw(field.getRawMeta(), false);

                String[] flags = new String[]{ "■", "□" };
                if (OPTION_CHECKBOX2.equals(thatFunc)) flags = new String[]{ "●", "○" };

                List<String> chk = new ArrayList<>();
                for (ConfigBean item : items) {
                    String itemText = item.getString("text");
                    if (ArrayUtils.contains(m, itemText)) chk.add(flags[0] + itemText);
                    else chk.add(flags[1] + itemText);
                }

                return StringUtils.join(chk, " ");
            }

        } else if (type == DisplayType.CLASSIFICATION) {
            if (thatFunc.startsWith(CLASS_PICK)) {
                int pickIndex = ObjectUtils.toInt(extractFuncValue(thatFunc)) - 1;
                String[] m = value.toString().split("\\.");

                if (pickIndex < 0) return m[m.length - 1];
                if (m.length > pickIndex) return m[pickIndex];
                return m[m.length - 1];  // last
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

        if (thatFunc.startsWith(IMAGE_SIZE)) {
            String[] wh = extractFuncValue(thatFunc).split("\\*");
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

            builder = Pictures.ofBytes(value).size(width, height > 0 ? height : width);
        }

        return builder.create();
    }

    // 提取函数附加值
    private static String extractFuncValue(String func) {
        // v3.7 兼容
        if (func.startsWith(IMAGE_SIZE) && !func.startsWith(IMAGE_SIZE + FVAL_SPLITER)) {
            func = IMAGE_SIZE + FVAL_SPLITER + func.substring(4);
        }

        String[] nn = func.split(FVAL_SPLITER);
        if (nn.length == 2) return nn[1];
        return StringUtils.EMPTY;
    }

    /**
     * @param varName
     * @return
     */
    public static String splitName(String varName) {
        return varName.split(FUNC_SPLITER)[0].trim();
    }

    /**
     * @param varName
     * @return
     */
    public static String splitFunc(String varName) {
        return varName.contains(FUNC_SPLITER) ? varName.split(FUNC_SPLITER)[1].trim() : null;
    }
}
