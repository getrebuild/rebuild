/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyDateTime extends EasyField {
    private static final long serialVersionUID = 3882003543084097603L;

    protected EasyDateTime(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
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
    public Object wrapValue(Object value) {
        String format = StringUtils.defaultIfBlank(
                getExtraAttr(FieldExtConfigProps.DATETIME_DATEFORMAT), getDisplayType().getDefaultFormat());
        return CalendarUtils.getDateFormat(format).format(value);
    }
}
