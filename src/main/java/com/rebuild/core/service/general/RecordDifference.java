/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.util.Map;

/**
 * 记录两个 Record 的不同
 *
 * @author devezhao
 * @since 2019/8/22
 */
public class RecordDifference {

    final private Record before;

    /**
     * @param before
     */
    protected RecordDifference(Record before) {
        this.before = before;
    }

    /**
     * 获取不同
     *
     * @param after
     * @return
     */
    public JSON merge(Record after) {
        if (before == null && after == null) {
            throw new RebuildException("Both records cannot be null");
        }

        if (before != null && after != null && !before.getEntity().equals(after.getEntity())) {
            throw new RebuildException("Both records must be the same entity");
        }

        Entity entity = before != null ? before.getEntity() : after.getEntity();
        Map<String, Object[]> merged = new CaseInsensitiveMap<>();

        if (before != null) {
            JSONObject beforeSerialize = (JSONObject) before.serialize();
            for (Map.Entry<String, Object> e : beforeSerialize.entrySet()) {
                String field = e.getKey();
                if (isIgnoreField(entity.getField(field))) {
                    continue;
                }

                Object beforeVal = e.getValue();
                if (NullValue.is(beforeVal)) {
                    beforeVal = null;
                }
                merged.put(field, new Object[]{beforeVal, null});
            }
        }

        if (after != null) {
            JSONObject afterSerialize = (JSONObject) after.serialize();
            for (Map.Entry<String, Object> e : afterSerialize.entrySet()) {
                String field = e.getKey();
                if (isIgnoreField(entity.getField(field))) {
                    continue;
                }

                Object afterVal = e.getValue();
                if (NullValue.is(afterVal)) {
                    continue;
                }

                Object[] mergedValue = merged.computeIfAbsent(field, k -> new Object[]{null, null});
                mergedValue[1] = afterVal;
            }
        }

        JSONArray array = new JSONArray();
        for (Map.Entry<String, Object[]> e : merged.entrySet()) {
            Object[] val = e.getValue();
            if (val[0] == null && val[1] == null) {
                continue;
            }

            JSON item = JSONUtils.toJSONObject(
                    new String[]{"field", "before", "after"},
                    new Object[]{e.getKey(), val[0], val[1]});
            array.add(item);
        }
        return array;
    }

    /**
     * 忽略字段
     *
     * @param field
     * @return
     */
    private boolean isIgnoreField(Field field) {
        String fieldName = field.getName();
        return EntityHelper.ModifiedOn.equalsIgnoreCase(fieldName)
                || EntityHelper.ModifiedBy.equalsIgnoreCase(fieldName)
                || EntityHelper.CreatedOn.equalsIgnoreCase(fieldName)
                || EntityHelper.CreatedBy.equalsIgnoreCase(fieldName)
                || EntityHelper.QuickCode.equalsIgnoreCase((fieldName))
                || MetadataHelper.isApprovalField(fieldName)
                || field.getType() == FieldType.PRIMARY;
    }
}
