/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.editor.BoolEditor;
import com.rebuild.core.support.i18n.Language;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyBool extends EasyField implements MixValue {
    private static final long serialVersionUID = -7818334967908628446L;

    protected EasyBool(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return (Boolean) value ? Language.L("True") : Language.L("False");
        }

        // Boolean
        return value;
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        return StringUtils.isBlank(valueExpr) ? Boolean.FALSE : BooleanUtils.toBoolean(valueExpr);
    }

    @Override
    public Object wrapValue(Object value) {
        return (Boolean) value ? BoolEditor.TRUE : BoolEditor.FALSE;
    }

    @Override
    public Object unpackWrapValue(Object wrappedValue) {
        if (wrappedValue instanceof Boolean) {
            return (Boolean) wrappedValue ? Language.L("True") : Language.L("False");
        }

        return StringUtils.equals(BoolEditor.TRUE + "", wrappedValue.toString())
                ? Language.L("True") : Language.L("False");
    }
}
