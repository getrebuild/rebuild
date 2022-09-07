/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.commons.ObjectUtils;
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
import java.util.Objects;

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
                String field = e.getKey();
                if (!diffCommons && isIgnoreField(entity.getField(field))) continue;

                Object beforeVal = e.getValue();
                if (NullValue.is(beforeVal)) beforeVal = null;

                merged.put(field, new Object[]{beforeVal, null});
            }
        }

        if (after != null) {
            JSONObject afterSerialize = (JSONObject) after.serialize();
            for (Map.Entry<String, Object> e : afterSerialize.entrySet()) {
                String field = e.getKey();
                if (!diffCommons && isIgnoreField(entity.getField(field))) continue;

                Object afterVal = e.getValue();
                if (NullValue.is(afterVal)) continue;

                Object[] mergedValue = merged.computeIfAbsent(field, k -> new Object[]{null, null});
                mergedValue[1] = afterVal;
            }
        }

        JSONArray result = new JSONArray();

        for (Map.Entry<String, Object[]> e : merged.entrySet()) {
            Object[] vals = e.getValue();
            if (vals[0] == null && vals[1] == null) continue;
            if (isEquals(vals[0], vals[1])) continue;

            JSON item = JSONUtils.toJSONObject(
                    new String[]{"field", "before", "after"},
                    new Object[]{e.getKey(), vals[0], vals[1]});
            result.add(item);
        }
        return result;
    }

    /**
     * 是否不同
     *
     * @param diff
     * @param diffCommons
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
                || (MetadataHelper.isApprovalField(fieldName) && !EntityHelper.ApprovalState.equalsIgnoreCase(fieldName))
                || field.getType() == FieldType.PRIMARY;
    }

    /**
     * 相等
     *
     * @param v1
     * @param v2
     * @return
     */
    private boolean isEquals(Object v1, Object v2) {
        boolean e = Objects.equals(v1, v2);
        if (!e) {
            if (v1 instanceof Number && v2 instanceof Number) {
                e = ObjectUtils.toDouble(v1) == ObjectUtils.toDouble(v2);
            }
        }
        return e;
    }
}
