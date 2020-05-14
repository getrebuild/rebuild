/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.api.BaseApi;
import com.rebuild.server.configuration.portals.ClassificationManager;
import com.rebuild.server.configuration.portals.MultiSelectManager;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;

/**
 * 获取字段列表
 *
 * @author devezhao
 * @since 2020/5/14
 */
public class FieldList extends BaseApi {

    @Override
    protected String getApiName() {
        return "metadata/fields";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        String entity = context.getParameterNotBlank("entity");
        Entity thatEntity = MetadataHelper.getEntity(entity);

        JSONArray array = new JSONArray();
        for (Field field : thatEntity.getFields()) {
            if (MetadataHelper.isSystemField(field) || !field.isQueryable()) {
                continue;
            }
            array.add(buildField(field));
        }
        return formatSuccess(array);
    }

    private JSONObject buildField(Field field) {
        final EasyMeta easyMeta = EasyMeta.valueOf(field);
        final DisplayType dt = easyMeta.getDisplayType();

        JSONObject o = new JSONObject();
        o.put("field_name", field.getName());
        o.put("field_label", easyMeta.getLabel());
        o.put("display_type", dt.name());

        o.put("creatable", field.isCreatable());
        o.put("updatable", field.isUpdatable());
        o.put("nullable", field.isNullable());
        o.put("repeatable", field.isRepeatable());
        o.put("queryable", field.isQueryable());

        if (dt == DisplayType.REFERENCE) {
            o.put("reference_entity", field.getReferenceEntity().getName());
        }
        if (dt == DisplayType.CLASSIFICATION) {
            o.put("use_classification", ClassificationManager.instance.getUseClassification(field, true));
        }
        if (dt == DisplayType.MULTISELECT) {
            o.put("options", MultiSelectManager.instance.getSelectList(field));
        }
        if (dt == DisplayType.PICKLIST) {
            o.put("options", PickListManager.instance.getPickList(field));
        }

        return o;
    }
}
