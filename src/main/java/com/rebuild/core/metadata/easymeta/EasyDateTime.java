/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang.StringUtils;

import java.util.Date;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyDateTime extends EasyField {
    private static final long serialVersionUID = 3882003543084097603L;

    protected EasyDateTime(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    /**
     * @param value
     * @param targetField
     * @param valueExpr
     * @return
     */
    public Object convertCompatibleValue(Object value, EasyField targetField, String valueExpr) {
        // 日期公式
        if (StringUtils.isNotBlank(valueExpr)) {
            Date newDate = FieldValueHelper.parseDateExpr("{NOW" + valueExpr + "}", (Date) value);
            if (newDate != null) {
                value = newDate;
            }
        }

        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return wrapValue(value);
        }

        String dateValue = (String) wrapValue(value);
        if (dateValue.length() == 4) {  // YYYY
            dateValue += "01-01 00:00:00";
        } else if (dateValue.length() == 7) {  // YYYY-MM
            dateValue += "-01 00:00:00";
        } else {  // YYYY-MM-DD
            dateValue += " 00:00:00";
        }
        return CalendarUtils.parse(dateValue);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        return convertCompatibleValue(value, targetField, null);
    }

    @Override
    public Object wrapValue(Object value) {
        String format = StringUtils.defaultIfBlank(
                getExtraAttr(EasyFieldConfigProps.DATETIME_FORMAT), getDisplayType().getDefaultFormat());
        return CalendarUtils.getDateFormat(format).format(value);
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        if (valueExpr == null) return null;

        // 表达式
        if (valueExpr.contains("NOW")) {
            return FieldValueHelper.parseDateExpr(valueExpr, null);
        }
        // 具体的日期值
        else {
            String format = "yyyy-MM-dd HH:mm:ss".substring(0, valueExpr.length());
            return CalendarUtils.parse(valueExpr, format);
        }
    }
}
