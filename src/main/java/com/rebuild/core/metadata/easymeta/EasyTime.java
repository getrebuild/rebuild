/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.record.RecordVisitor;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

/**
 * @author devezhao
 * @since 2022/03/16
 */
public class EasyTime extends EasyDateTime {
    private static final long serialVersionUID = -3204466240074143268L;

    protected EasyTime(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return wrapValue(value);
        }

        // LocalTime
        return value;
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        if (StringUtils.isBlank(valueExpr)) return null;

        // 表达式
        if (valueExpr.contains(VAR_NOW) || valueExpr.contains("NOW")) {
            Date d = FieldValueHelper.parseDateExpr(valueExpr, null);
            if (d == null) return null;
            Calendar c = CalendarUtils.getInstance(d);
            return LocalTime.of(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        }
        // 具体的时间值
        else {
            return RecordVisitor.tryParseTime(valueExpr);
        }
    }

    @Override
    public Object wrapValue(Object value) {
        String format = StringUtils.defaultIfBlank(
                getExtraAttr(EasyFieldConfigProps.TIME_FORMAT), getDisplayType().getDefaultFormat());
        return DateTimeFormatter.ofPattern(format).format((TemporalAccessor) value);
    }
}
