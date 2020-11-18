/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyDecimal extends EasyField {
    private static final long serialVersionUID = -2496779775401532438L;

    protected EasyDecimal(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return wrapValue(value);
        }

        if (targetType == DisplayType.NUMBER) {
            return ObjectUtils.toLong(value);
        }

        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object wrapValue(Object value) {
        return new DecimalFormat(attrFormat()).format(value);
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        return valueExpr == null
                ? null : BigDecimal.valueOf(NumberUtils.toDouble(valueExpr));
    }

    /**
     * 允许负数
     *
     * @return
     */
    public boolean attrNotNegative() {
        String attr = getExtraAttr(EasyFieldConfigProps.DECIMAL_NOTNEGATIVE);
        return attr == null || BooleanUtils.toBoolean(attr);
    }

    /**
     * 格式化
     *
     * @return
     */
    public String attrFormat() {
        return StringUtils.defaultIfBlank(
                getExtraAttr(EasyFieldConfigProps.DECIMAL_FORMAT), getDisplayType().getDefaultFormat());
    }
}
