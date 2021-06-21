/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateHelper;
import com.rebuild.core.support.state.StateSpec;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyState extends EasyField implements MixValue {
    private static final long serialVersionUID = -5160207555364899330L;

    protected EasyState(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return unpackWrapValue(value);
        }

        // Integer
        return value;
    }

    @Override
    public Object exprDefaultValue() {
        Class<?> stateClass = StateHelper.getSatetClass(getRawMeta());
        for (Object c : stateClass.getEnumConstants()) {
            if (((StateSpec) c).isDefault()) {
                return ((StateSpec) c).getState();
            }
        }
        return null;
    }

    @Override
    public Object unpackWrapValue(Object wrappedValue) {
        Class<?> stateClass = StateHelper.getSatetClass(getRawMeta());
        String rawName = StateHelper.valueOf(stateClass, (Integer) wrappedValue).getName();
        return Language.L(rawName);
    }

    @Override
    public JSON toJSON() {
        JSONObject map = (JSONObject) super.toJSON();
        map.put(EasyFieldConfigProps.STATE_CLASS, getExtraAttr(EasyFieldConfigProps.STATE_CLASS));
        return map;
    }
}
