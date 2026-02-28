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
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.md.MarkdownUtils;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static cn.devezhao.commons.DateFormatUtils.CN_DATETIME_FORMAT;

/**
 * @author devezhao
 * @since 2024/6/25
 */
@Slf4j
public class ValueFnConvert {

    // # 函数分隔符
    private static final String FN_SPLITER = "#";
    private static final String FNVAL_SPLITER = ":";
    // 支持的函数
    private static final String CHINESE_4DATE_NUM = "CHINESE";
    private static final String CHINESEYUAN_4NUM = "CHINESEYUAN";
    private static final String THOUSANDS_4NUM = "THOUSANDS";
    private static final String CHECKBOX_4OPTION = "CHECKBOX";
    private static final String CHECKBOX2_4OPTION = "CHECKBOX" + FNVAL_SPLITER + "2";
    private static final String PICKAT_4CLASS_DATE = "PICKAT";  // eg. PICKAT:2
    private static final String SIZE_4IMG = "SIZE";  // eg. SIZE:100*200, SIZE:100
    private static final String EMPTY = "EMPTY";  // eg. EMPTY:无
    public static final String CLEAR_4NTEXT = "CLEAR";
    private static final String FORMAT_4DATE = "FORMAT";  // eg. FORMAT:MM/dd

    /**
     * @param field
     * @param value
     * @param varName
     * @param fromClazz
     * @return
     */
    public static Object convert(EasyField field, Object value, String varName, Class<?> fromClazz) {
        String theFn = splitFn(varName);
        if (theFn == null) return value;

        // 默认值
        if (theFn.startsWith(EMPTY)) {
            if (value == null || StringUtils.isBlank(value.toString())) {
                return extractFnValue(theFn);
            }
        }

        final DisplayType type = field.getDisplayType();

        // 空值也处理
        if (type == DisplayType.MULTISELECT || type == DisplayType.PICKLIST) {
            if (CHECKBOX_4OPTION.equals(theFn) || CHECKBOX2_4OPTION.equals(theFn)) {
                String[] m = value == null ? new String[0] : value.toString().split(", ");
                ConfigBean[] items = MultiSelectManager.instance.getPickListRaw(field.getRawMeta(), false);

                String[] flags = new String[]{"■", "□"};
                if (CHECKBOX2_4OPTION.equals(theFn)) flags = new String[]{"●", "○"};

                List<String> chk = new ArrayList<>();
                for (ConfigBean item : items) {
                    String itemText = item.getString("text");
                    if (ArrayUtils.contains(m, itemText)) chk.add(flags[0] + itemText);
                    else chk.add(flags[1] + itemText);
                }

                return StringUtils.join(chk, " ");
            }
        }

        if (value == null) return null;

        if (type == DisplayType.NUMBER || type == DisplayType.DECIMAL) {
            switch (theFn) {
                case CHINESEYUAN_4NUM:
                    return Convert.digitToChinese((Number) value);
                case CHINESE_4DATE_NUM:
                    return Convert.numberToChinese(((Number) value).doubleValue(), true);
                case THOUSANDS_4NUM:
                    String format = "##,##0";
                    if (type == DisplayType.DECIMAL) {
                        int scale = ((EasyDecimal) field).getScale();
                        if (scale > 0) {
                            format += "." + StringUtils.leftPad("", scale, "0");
                        }
                    }
                    return new DecimalFormat(format).format(value);
            }

        } else if (type == DisplayType.DATE || type == DisplayType.DATETIME || type == DisplayType.TIME) {
            if (CHINESE_4DATE_NUM.equals(theFn) || "CHINESEDATE".equals(theFn)) {
                if (type == DisplayType.TIME) {
                    String s = "2024-01-01 " + value;
                    Date d = CommonsUtils.parseDate(s);
                    if (d == null) return value;

                    int len = field.wrapValue(LocalTime.now()).toString().length() + 1;
                    String format = CalendarUtils.CN_TIME_FORMAT.substring(0, len);
                    return CalendarUtils.getDateFormat(format).format(d);
                } else {
                    Date d = CommonsUtils.parseDate(value.toString());
                    if (d == null) return value;

                    String fieldFormat = field.getExtraAttr(EasyFieldConfigProps.DATETIME_FORMAT);
                    if (fieldFormat == null) fieldFormat = field.getExtraAttr(EasyFieldConfigProps.DATE_FORMAT);
                    if (fieldFormat == null) fieldFormat = field.getDisplayType().getDefaultFormat();
                    int len = 5;  // 年
                    if (fieldFormat.contains("ss")) len = CN_DATETIME_FORMAT.length();            // 秒
                    else if (fieldFormat.contains("mm")) len = CN_DATETIME_FORMAT.length() - 3;   // 分
                    else if (fieldFormat.contains("HH")) len = CN_DATETIME_FORMAT.length() - 6;   // 时
                    else if (fieldFormat.contains("dd")) len = CN_DATETIME_FORMAT.length() - 10;  // 日
                    else if (fieldFormat.contains("MM")) len = CN_DATETIME_FORMAT.length() - 13;  // 月

                    String format = CN_DATETIME_FORMAT.substring(0, len);
                    return CalendarUtils.getDateFormat(format).format(d);
                }

            } else if (theFn.startsWith(PICKAT_4CLASS_DATE) && type != DisplayType.TIME) {
                String[] m = value.toString().replace(" ", "-").split("[-:]");
                int pickIndex = ObjectUtils.toInt(extractFnValue(theFn)) - 1;

                if (pickIndex < 0) return m[0];
                if (m.length > pickIndex) return m[pickIndex];
                return m[0];  // first

            } else if (theFn.startsWith(FORMAT_4DATE)) {
                if (type == DisplayType.TIME) {
                    value = "2024-01-01 " + value;
                }
                Date d = CommonsUtils.parseDate(value.toString());
                if (d == null) return value;

                String format = extractFnValue(theFn);
                return CalendarUtils.format(format, d);
            }

        } else if (type == DisplayType.CLASSIFICATION) {
            if (theFn.startsWith(PICKAT_4CLASS_DATE)) {
                int pickIndex = ObjectUtils.toInt(extractFnValue(theFn)) - 1;
                String[] m = value.toString().split("\\.");

                if (pickIndex < 0) return m[m.length - 1];
                if (m.length > pickIndex) return m[pickIndex];
                return m[m.length - 1];  // last
            }

        } else if (type == DisplayType.NTEXT) {
            if (theFn.equals(CLEAR_4NTEXT)) {
                if (fromClazz != null && fromClazz.getSimpleName().equals("Html5ReportGenerator")) {
                    String md2html = MarkdownUtils.render((String) value);
                    return "<div class='md-content md2html'>" + md2html + "</div>";
                } else {
                    return MarkdownUtils.cleanMarks((String) value);
                }
            }
        }

        return value;
    }

    /**
     * 转换图片（WORD 格式）
     *
     * @param value
     * @param varName
     * @return
     */
    public static PictureRenderData convertPictureWithSize(byte[] value, String varName) {
        Pictures.PictureBuilder builder = Pictures.ofBytes(value);

        String thatFunc = splitFn(varName);
        if (thatFunc == null) return builder.create();

        if (thatFunc.startsWith(SIZE_4IMG)) {
            String[] wh = extractFnValue(thatFunc).split("\\*");
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
    private static String extractFnValue(String fn) {
        // v3.7 兼容
        if (fn.startsWith(SIZE_4IMG) && !fn.startsWith(SIZE_4IMG + FNVAL_SPLITER)) {
            fn = SIZE_4IMG + FNVAL_SPLITER + fn.substring(4);
        }

        String[] nn = fn.split(FNVAL_SPLITER);
        if (nn.length == 2) return nn[1];
        return StringUtils.EMPTY;
    }

    /**
     * @param varName
     * @return
     */
    public static String splitName(String varName) {
        return varName.split(FN_SPLITER)[0].trim();
    }

    /**
     * @param varName
     * @return
     */
    public static String splitFn(String varName) {
        return varName.contains(FN_SPLITER) ? varName.split(FN_SPLITER)[1].trim() : null;
    }
}
