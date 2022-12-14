/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.support.general.N2NReferenceSupport;
import com.rebuild.core.support.general.TagSupport;
import org.apache.commons.lang.StringUtils;

/**
 * @author Zixin
 * @since 2022/12/12
 * @see N2NReferenceSupport
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
        return super.convertCompatibleValue(value, targetField);
    }

    @Override
    public Object exprDefaultValue() {
        return TagSupport.getDefaultValue(this);
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
        return StringUtils.join((JSONArray) wrappedValue, MV_SPLIT);
    }
}
