/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.utils.CommonsUtils;
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
            return CommonsUtils.toLongHalfUp(value);
        }

        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        return StringUtils.isBlank(valueExpr)
                ? null : BigDecimal.valueOf(NumberUtils.toDouble(valueExpr));
    }

    @Override
    public Object wrapValue(Object value) {
        String format = StringUtils.defaultIfBlank(
                getExtraAttr(EasyFieldConfigProps.DECIMAL_FORMAT), getDisplayType().getDefaultFormat());
        String n = new DecimalFormat(format).format(value);

        // 0, %, etc.
        String type = getExtraAttr(EasyFieldConfigProps.DECIMAL_TYPE);
        if (type != null && !"0".equals(type)) {
            if ("%".equals(type)) n += "%";
            else if (type.contains("%s")) n = String.format(type, n); // %s万元
            else n = type + " " + n;
        }
        return n;
    }
}
