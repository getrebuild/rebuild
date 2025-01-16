/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.configuration.general.LiteFormBuilder;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可编辑字段
 * 
 * @author ZiXin
 * @since 2015/1/15
 */
public class EditableFields {
    
    private final JSONArray editableFields;
    
    public EditableFields(JSONArray editableFields) {
        this.editableFields = editableFields;
    }

    /**
     * @param recordId
     * @param user
     * @return
     */
    public JSONObject buildForms(ID recordId, ID user) {
        final Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

        Map<String, JSONArray> fieldsByEntity = getEditableFieldsByEntity(entity.getName());
        JSONObject aforms = new JSONObject();

        JSONArray mFields = fieldsByEntity.remove(entity.getName());
        if (CollectionUtils.isNotEmpty(mFields)) {
            JSONArray aform = new LiteFormBuilder(recordId, user).build(mFields);
            if (CollectionUtils.isNotEmpty(aform)) {
                aforms.put("aform", aform);
                aforms.put("aentity", entity.getName());
            }
        }

        List<JSONObject> detailsByEntity = new ArrayList<>();
        for (Map.Entry<String, JSONArray> e : fieldsByEntity.entrySet()) {
            Entity dEntity = MetadataHelper.getEntity(e.getKey());
            JSONArray dFields = e.getValue();

            JSONArray dForms = new JSONArray();
            for (ID did : QueryHelper.detailIdsNoFilter(recordId, dEntity)) {
                JSONArray aform = new LiteFormBuilder(did, user).build(dFields);
                if (CollectionUtils.isNotEmpty(aform)) {
                    aform.add(did);  // Last is ID
                    dForms.add(aform);
                }
            }

            if (!dForms.isEmpty()) {
                JSONObject d = JSONUtils.toJSONObject(
                        new String[]{"aentity", "aentityLabel", "aforms"},
                        new Object[]{dEntity.getName(), EasyMetaFactory.getLabel(dEntity), dForms});
                detailsByEntity.add(d);
            }
        }

        if (!detailsByEntity.isEmpty()) {
            aforms.put("aform_details", detailsByEntity);
        }

        return aforms;
    }

    private Map<String, JSONArray> getEditableFieldsByEntity(String entityName) {
        Map<String, JSONArray> fieldsByEntity = new HashMap<>();
        for (Object o : editableFields) {
            JSONObject item = (JSONObject) o;
            String fieldName = item.getString("field");
            String[] ef;
            if (fieldName.contains(".")) ef = fieldName.split("\\.");
            else ef = new String[]{entityName, fieldName};

            JSONArray fields = fieldsByEntity.computeIfAbsent(ef[0], k -> new JSONArray());
            item.put("field", ef[1]);
            fields.add(item);
        }
        return fieldsByEntity;
    }
}
