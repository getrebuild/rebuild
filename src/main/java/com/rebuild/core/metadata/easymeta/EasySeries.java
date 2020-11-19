/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasySeries extends EasyField {
    private static final long serialVersionUID = 2947282784021933768L;

    protected EasySeries(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return wrapValue(value);
        }

        throw new UnsupportedOperationException("auto value");
    }

    @Override
    public Object exprDefaultValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return
     */
    public String attrFormat() {
        return StringUtils.defaultIfBlank(
                getExtraAttr(EasyFieldConfigProps.SERIES_FORMAT), getDisplayType().getDefaultFormat());
    }

    /**
     * @return
     */
    public String attrZeroMode() {
        return getExtraAttr(EasyFieldConfigProps.SERIES_ZERO);
    }
}
