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
public class EasyDate extends EasyDateTime {
    private static final long serialVersionUID = -4939751787442232744L;

    protected EasyDate(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object wrapValue(Object value) {
        String format = StringUtils.defaultIfBlank(
                getExtraAttr(FieldExtConfigProps.DATE_DATEFORMAT), getDisplayType().getDefaultFormat());
        return CalendarUtils.getDateFormat(format).format(value);
    }
}
