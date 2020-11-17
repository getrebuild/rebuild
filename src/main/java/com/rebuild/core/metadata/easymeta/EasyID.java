/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyID extends EasyField {
    private static final long serialVersionUID = -8790411385069895967L;

    protected EasyID(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return wrapValue(value);
        }

        ID idValue = (ID) value;
        if (targetType == DisplayType.REFERENCE) {
            return idValue;
        }
        if (targetType == DisplayType.N2NREFERENCE) {
            return new ID[] { idValue };
        }

        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object exprDefaultValue() {
        throw new UnsupportedOperationException();
    }
}
