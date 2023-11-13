/*!
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
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.util.Map;

/**
 * 两个 Record 的不同
 *
 * @author devezhao
 * @since 2019/8/22
 */
public class RecordDifference {

    final private Record record;

    /**
     * @param record
     */
    public RecordDifference(Record record) {
        this.record = record;
    }

    /**
     * 获取不同
     *
     * @param after
     * @return
     */
    public JSON diffMerge(Record after) {
        return diffMerge(after, false);
    }

    /**
     * 获取不同
     *
     * @param after
     * @param diffCommons
     * @return
     */
    protected JSON diffMerge(Record after, boolean diffCommons) {
        if (record == null && after == null) {
            throw new RebuildException("Both records cannot be null");
        }

        if (record != null && after != null && !record.getEntity().equals(after.getEntity())) {
            throw new RebuildException("Both records must be the same entity");
        }

        Entity entity = record != null ? record.getEntity() : after.getEntity();
        Map<String, Object[]> merged = new CaseInsensitiveMap<>();

        if (record != null) {
            JSONObject recordSerialize = (JSONObject) record.serialize();
            for (Map.Entry<String, Object> e : recordSerialize.entrySet()) {
                String fieldName = e.getKey();
                if (!entity.containsField(fieldName)) continue;
                if (!diffCommons && isIgnoreField(entity.getField(fieldName))) continue;

                Object beforeVal = e.getValue();
                if (NullValue.is(beforeVal)) beforeVal = null;

                merged.put(fieldName, new Object[]{beforeVal, null});
            }
        }

        if (after != null) {
            JSONObject afterSerialize = (JSONObject) after.serialize();
            for (Map.Entry<String, Object> e : afterSerialize.entrySet()) {
                String fieldName = e.getKey();
                if (!entity.containsField(fieldName)) continue;
                if (!diffCommons && isIgnoreField(entity.getField(fieldName))) continue;

                Object afterVal = e.getValue();
                if (NullValue.is(afterVal)) continue;

                Object[] mergedValue = merged.computeIfAbsent(fieldName, k -> new Object[]{null, null});
                mergedValue[1] = afterVal;
            }
        }

        JSONArray result = new JSONArray();

        for (Map.Entry<String, Object[]> e : merged.entrySet()) {
            Object[] vals = e.getValue();
            if (vals[0] == null && vals[1] == null) continue;
            if (CommonsUtils.isSame(vals[0], vals[1])) continue;

            JSON item = JSONUtils.toJSONObject(
                    new String[]{"field", "before", "after"},
                    new Object[]{e.getKey(), vals[0], vals[1]});
            result.add(item);
        }
        return result;
    }

    /**
     * 是否相同
     *
     * @param diff
     * @param diffCommons 是否比较系统共用字段
     * @return
     * @see #diffMerge(Record)
     */
    public boolean isSame(Record diff, boolean diffCommons) {
        JSONArray result = (JSONArray) diffMerge(diff, diffCommons);
        return result.isEmpty();
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
                || field.getType() == FieldType.PRIMARY
                || MetadataHelper.isApprovalField(fieldName);
    }
}
