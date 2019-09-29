/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.metadata;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import com.rebuild.server.configuration.portals.MultiSelectManager;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.helper.state.StateHelper;
import com.rebuild.server.helper.state.StateSpec;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段默认值
 *
 * @author devezhao
 * @since 2019/8/20
 */
public class DefaultValueHelper {

    private static final Log LOG = LogFactory.getLog(DefaultValueHelper.class);

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
                if (((StateSpec) c).isDefault()) return ((StateSpec) c).getState();
            }
        } else if (dt == DisplayType.MULTISELECT) {
            return MultiSelectManager.instance.getDefaultValue(field);
        }

        if (StringUtils.isBlank(valueExpr)) {
            return null;
        }

        if (dt == DisplayType.DATE || dt == DisplayType.DATETIME) {
            if ("{NOW}".equals(valueExpr)) {
                return CalendarUtils.now();
            }

            Pattern exprPattern = Pattern.compile("\\{NOW([-+])([0-9]{1,9})([YMDH])\\}");
            Matcher exprMatcher = exprPattern.matcher(StringUtils.remove(valueExpr, " "));
            if (exprMatcher.matches()) {
                String op = exprMatcher.group(1);
                String num = exprMatcher.group(2);
                String unit = exprMatcher.group(3);
                int num2int = ObjectUtils.toInt(num);
                if ("-".equals(op)) {
                    num2int = -num2int;
                }

                Date date = null;
                if (num2int == 0) {
                    date = CalendarUtils.now();
                } else if ("Y".equals(unit)) {
                    date = CalendarUtils.add(num2int, Calendar.YEAR);
                } else if ("M".equals(unit)) {
                    date = CalendarUtils.add(num2int, Calendar.MONTH);
                } else if ("D".equals(unit)) {
                    date = CalendarUtils.add(num2int, Calendar.DAY_OF_MONTH);
                } else if ("H".equals(unit)) {
                    date = CalendarUtils.add(num2int, Calendar.HOUR_OF_DAY);
                }
                return date;
            } else {
                String format = "yyyy-MM-dd HH:mm:ss".substring(0, valueExpr.length());
                return CalendarUtils.parse(valueExpr, format);
            }

        } else if (dt == DisplayType.DECIMAL) {
            return BigDecimal.valueOf(NumberUtils.toDouble(valueExpr));
        } else if (dt == DisplayType.NUMBER) {
            return NumberUtils.toLong(valueExpr);
        }
        else {
            return valueExpr;
        }
    }
}
