/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyNumber extends EasyField {
    private static final long serialVersionUID = 4834958616359008836L;

    protected EasyNumber(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return wrapValue(value);
        }

        if (targetType == DisplayType.DECIMAL) {
            return BigDecimal.valueOf((Long) value);
        }

        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        return StringUtils.isBlank(valueExpr) ? null : NumberUtils.toLong(valueExpr);
    }

    @Override
    public Object wrapValue(Object value) {
        String format = StringUtils.defaultIfBlank(
                getExtraAttr(EasyFieldConfigProps.NUMBER_FORMAT), getDisplayType().getDefaultFormat());
        return new DecimalFormat(format).format(value);
    }
}
