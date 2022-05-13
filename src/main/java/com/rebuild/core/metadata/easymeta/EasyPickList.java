/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.PickListManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @author devezhao
 * @since 2020/11/17
 */
@Slf4j
public class EasyPickList extends EasyField implements MixValue {
    private static final long serialVersionUID = 5971173892145230003L;

    protected EasyPickList(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        ID idValue = (ID) value;
        String text = PickListManager.instance.getLabel(idValue);

        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return text;
        }

        ID itemId = PickListManager.instance.findItemByLabel(text, targetField.getRawMeta());
        if (itemId == null) {
            log.warn("Cannot find value in PickList : " + text + " << " + targetField);
        }
        return itemId;
    }

    @Override
    public Object exprDefaultValue() {
        return PickListManager.instance.getDefaultItem(getRawMeta());
    }

    @Override
    public Object unpackWrapValue(Object wrappedValue) {
        return PickListManager.instance.getLabel((ID) wrappedValue);
    }

    @Override
    public JSON toJSON() {
        JSONObject map = (JSONObject) super.toJSON();
        map.remove("ref");
        return map;
    }
}
