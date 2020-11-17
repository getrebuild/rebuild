/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyN2NReference extends EasyReference {
    private static final long serialVersionUID = -16180408450167432L;

    protected EasyN2NReference(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object wrapValue(Object value) {
        ID[] idArrayValue = (ID[]) value;
        JSONArray array = new JSONArray();
        for (ID id : idArrayValue) {
            array.add(super.wrapValue(id));
        }
        return array;
    }

    @Override
    public Object unpackWrapValue(Object wrappedValue) {
        JSONArray array = (JSONArray) wrappedValue;
        return array.isEmpty() ? null : ((JSONObject) array.get(0)).getString("text");
    }
}
