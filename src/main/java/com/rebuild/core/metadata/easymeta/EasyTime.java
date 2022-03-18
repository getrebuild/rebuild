/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.record.RecordVisitor;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.apache.commons.lang.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

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

        if (valueExpr.contains("NOW")) {  // {NOW}
            return LocalTime.now();
        } else {
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
