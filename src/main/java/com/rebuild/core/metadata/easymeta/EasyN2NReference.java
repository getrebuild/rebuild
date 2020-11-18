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
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;

        ID[] idArrayValue = (ID[]) value;

        if (is2Text) {
            List<String> array = new ArrayList<>();
            for (ID id : idArrayValue) {
                array.add((String) super.convertCompatibleValue(id, targetField));
            }
            return StringUtils.join(array, ", ");

        } else {
            return idArrayValue;
        }
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
        JSONArray arrayValue = (JSONArray) wrappedValue;

        List<String> array = new ArrayList<>();
        for (Object item : arrayValue) {
            array.add(((JSONObject) item).getString ("text"));
        }
        return StringUtils.join(array, ", ");
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        if (StringUtils.isBlank(valueExpr)) return null;

        List<ID> idArray = new ArrayList<>();
        for (String id : valueExpr.split(",")) {
            if (ID.isId(id)) idArray.add(ID.valueOf(id));
        }
        return idArray.toArray(new ID[0]);
    }

    @Override
    public String attrDataFilter() {
        return getExtraAttr(EasyFieldConfigProps.N2NREFERENCE_DATAFILTER);
    }
}
