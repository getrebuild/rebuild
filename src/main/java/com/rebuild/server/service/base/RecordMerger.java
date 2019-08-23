/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.RebuildException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.util.Map;

/**
 * 记录合并
 *
 * @author devezhao
 * @since 2019/8/22
 */
public class RecordMerger {

    private Record beforeRecord;

    /**
     * @param beforeRecord
     */
    protected RecordMerger(Record beforeRecord) {
        this.beforeRecord = beforeRecord;
    }

    /**
     * @param afterRecord
     * @return
     */
    public JSON merge(Record afterRecord) {
        if (beforeRecord == null && afterRecord == null) {
            throw new RebuildException("Both records cannot be null");
        }
        if (beforeRecord != null && afterRecord != null && !beforeRecord.getEntity().equals(afterRecord.getEntity())) {
            throw new RebuildException("Both records must be the same entity");
        }

        Entity entity = beforeRecord != null ? beforeRecord.getEntity() : afterRecord.getEntity();
        Map<String, Object[]> merged = new CaseInsensitiveMap<>();

        if (beforeRecord != null) {
            JSONObject beforeSerialize = (JSONObject) beforeRecord.serialize();
            for (Map.Entry<String, Object> e : beforeSerialize.entrySet()) {
                String field = e.getKey();
                if (isIgnoreField(entity.getField(field))) {
                    continue;
                }

                Object beforeVal = e.getValue();
                if (NullValue.is(beforeVal)) {
                    beforeVal = null;
                }
                merged.put(field, new Object[]{ beforeVal, null });
            }
        }

        if (afterRecord != null) {
            JSONObject afterSerialize = (JSONObject) afterRecord.serialize();
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
                    new String[] { "field", "before", "after" },
                    new Object[] { e.getKey(), val[0], val[1] });
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
