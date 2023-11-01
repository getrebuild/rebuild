/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zixin
 * @since 2022/12/12
 * @see EasyN2NReference
 * @see EasyMultiSelect
 */
public class EasyTag extends EasyField implements MultiValue, MixValue {
    private static final long serialVersionUID = -5827184319679918289L;

    public static final String VALUE_SPLIT = MetadataHelper.SPLITER;
    public static final String VALUE_SPLIT_RE = MetadataHelper.SPLITER_RE;

    protected EasyTag(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return unpackWrapValue(value);
        }

        Assert.isTrue(targetField.getDisplayType() == getDisplayType(), "type-by-type is must");
        return value;
    }

    @Override
    public Object exprDefaultValue() {
        JSONArray tagList = getExtraAttrs(true).getJSONArray(EasyFieldConfigProps.TAG_LIST);
        if (tagList == null || tagList.isEmpty()) return null;

        List<String> dv = new ArrayList<>();
        for (Object o : tagList) {
            JSONObject tag = (JSONObject) o;
            if (tag.getBooleanValue("default")) dv.add(tag.getString("name"));
        }
        return dv.isEmpty() ? null : dv.toArray(new String[0]);
    }

    @Override
    public Object wrapValue(Object value) {
        if (value == null) return null;
        if (value instanceof String) return value.toString();  // When single-edit
        if (((String[]) value).length == 0) return null;

        return JSON.toJSON(value);
    }

    @Override
    public Object unpackWrapValue(Object wrappedValue) {
        if (wrappedValue instanceof String[]) return StringUtils.join((String[]) wrappedValue, MV_SPLIT);
        return StringUtils.join((JSONArray) wrappedValue, MV_SPLIT);
    }
}
