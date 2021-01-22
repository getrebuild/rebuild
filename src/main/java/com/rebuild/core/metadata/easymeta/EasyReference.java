/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.support.general.FieldValueHelper;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyReference extends EasyField implements MixValue {
    private static final long serialVersionUID = -5001745527956303569L;

    protected EasyReference(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return FieldValueHelper.getLabelNotry((ID) value);
        }

        // ID
        return value;
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        return ID.isId(valueExpr) ? ID.valueOf(valueExpr) : null;
    }

    @Override
    public Object wrapValue(Object value) {
        ID idValue = (ID) value;
        Object text = idValue.getLabelRaw();
        if (text == null) {
            text = FieldValueHelper.getLabelNotry(idValue);
        }

        return FieldValueHelper.wrapMixValue(idValue, text == null ? null : text.toString());
    }

    @Override
    public JSON toJSON() {
        JSONObject map = (JSONObject) super.toJSON();

        Entity refEntity = getRawMeta().getReferenceEntity();
        map.put("ref", new String[] { refEntity.getName(),
                EasyMetaFactory.getDisplayType(refEntity.getNameField()).name() });
        return map;
    }
}
