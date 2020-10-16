/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.core.configuration.general.MultiSelectManager;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.core.support.state.StateSpec;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段默认值处理
 *
 * @author devezhao
 * @since 2019/8/20
 */
public class DefaultValueHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultValueHelper.class);

    // 日期公式 {NOW + 1D}
    private static final Pattern EXPR_PATTERN = Pattern.compile("\\{NOW([-+])([0-9]{1,9})([YMDHI])}");

    /**
     * 获取字段默认值
     *
     * @param field
     * @return
     */
    public static String exprDefaultValueToString(Field field) {
        Object defVal = exprDefaultValue(field, (String) field.getDefaultValue());
        if (defVal == null) {
            return null;
        }

        if (field.getType() == FieldType.TIMESTAMP || field.getType() == FieldType.DATE) {
            return CalendarUtils.getUTCDateTimeFormat().format(defVal);
        } else {
            return defVal.toString();
        }
    }

    /**
     * 获取字段默认值，可指定表达式
     *
     * @param field
     * @param valueExpr
     * @return
     */
    public static Object exprDefaultValue(Field field, String valueExpr) {
        final DisplayType dt = EasyMeta.getDisplayType(field);

        if (dt == DisplayType.PICKLIST) {
            return PickListManager.instance.getDefaultItem(field);
        } else if (dt == DisplayType.STATE) {
            Class<?> stateClass;
            try {
                stateClass = StateHelper.getSatetClass(field);
            } catch (IllegalArgumentException ex) {
                LOG.error("Bad field of state: " + field);
                return null;
            }

            for (Object c : stateClass.getEnumConstants()) {
                if (((StateSpec) c).isDefault()) {
                    return ((StateSpec) c).getState();
                }
            }
        } else if (dt == DisplayType.MULTISELECT) {
            return MultiSelectManager.instance.getDefaultValue(field);
        }

        if (StringUtils.isBlank(valueExpr)) {
            return null;
        }

        if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
            Date date;
            if (valueExpr.contains("NOW")) {
                date = parseDateExpr(valueExpr, null);
            }
            // 具体的日期值
            else {
                String format = "yyyy-MM-dd HH:mm:ss".substring(0, valueExpr.length());
                date = CalendarUtils.parse(valueExpr, format);
            }
            return date;

        } else if (dt == DisplayType.DECIMAL) {
            return BigDecimal.valueOf(NumberUtils.toDouble(valueExpr));
        } else if (dt == DisplayType.NUMBER) {
            return NumberUtils.toLong(valueExpr);
        } else {
            return valueExpr;
        }
    }

    /**
     * 解析日期表达式
     *
     * @param dateExpr
     * @param base
     * @return
     */
    public static Date parseDateExpr(String dateExpr, Date base) {
        if ("{NOW}".equals(dateExpr)) {
            return CalendarUtils.now();
        }

        Matcher m = EXPR_PATTERN.matcher(StringUtils.remove(dateExpr, " "));
        if (m.matches()) {
            base = base == null ? CalendarUtils.now() : base;

            String op = m.group(1);
            String num = m.group(2);
            String unit = m.group(3);
            int num2int = ObjectUtils.toInt(num);
            if ("-".equals(op)) {
                num2int = -num2int;
            }

            Date date = null;
            if (num2int == 0) {
                date = base;
            } else if ("Y".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.YEAR);
            } else if ("M".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.MONTH);
            } else if ("D".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.DAY_OF_MONTH);
            } else if ("H".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.HOUR_OF_DAY);
            } else if ("I".equals(unit)) {
                date = CalendarUtils.add(base, num2int, Calendar.MINUTE);
            }
            return date;
        }

        return null;
    }
}
