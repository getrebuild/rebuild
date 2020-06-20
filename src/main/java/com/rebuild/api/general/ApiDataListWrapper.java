/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.dialect.editor.BoolEditor;
import cn.devezhao.persist4j.engine.NullValue;
import cn.devezhao.persist4j.query.compiler.SelectItem;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.datalist.DataListWrapper;
import com.rebuild.server.helper.fieldvalue.FieldValueWrapper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;

/**
 * @author devezhao
 * @since 2020/5/21
 */
public class ApiDataListWrapper extends DataListWrapper {

    /**
     * @param total
     * @param data
     * @param query
     */
    protected ApiDataListWrapper(int total, Object[][] data, Query query) {
        super(total, data, query.getSelectItems(), query.getRootEntity());
    }

    @Override
    public JSON toJson() {
        JSONArray list = new JSONArray();
        for (Object[] row : data) {
            list.add(buildItem(selectFields, row));
        }

        return JSONUtils.toJSONObject(
                new String[] { "total", "list" },
                new Object[] { total, list });
    }

    /**
     * @param fields
     * @param data
     * @return
     */
    static JSON buildItem(SelectItem[] fields, Object[] data) {
        JSONObject item = new JSONObject();

        for (int i = 0; i < fields.length; i++) {
            final String name = fields[i].getField().getName();
            final Object value = data[i];
            if (value == null || NullValue.is(value)) {
                item.put(name, null);
                continue;
            }

            Object displayValue = FieldValueWrapper.instance.wrapFieldValue(value, fields[i].getField(), false);

            DisplayType dt = EasyMeta.getDisplayType(fields[i].getField());
            if (dt == DisplayType.MULTISELECT || dt == DisplayType.PICKLIST || dt == DisplayType.STATE) {
                displayValue = JSONUtils.toJSONObject(new String[] { "value", "text" }, new Object[] { value, displayValue });
            } else if (dt == DisplayType.BOOL) {
                displayValue = (Boolean) value ? BoolEditor.TRUE : BoolEditor.FALSE;
            }
            item.put(name, displayValue);

        }
        return item;
    }
}
