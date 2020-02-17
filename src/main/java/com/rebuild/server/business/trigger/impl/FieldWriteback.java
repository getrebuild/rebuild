/*
rebuild - Building your business-systems freely.
Copyright (C) 2020 devezhao <zhaofang123@gmail.com>

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
For more information, please see <https://getrebuild.com>
*/

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据回填
 *
 * @author devezhao
 * @since 2020/2/7
 *
 * @see com.rebuild.server.configuration.AutoFillinManager
 */
public class FieldWriteback extends FieldAggregation {

    /**
     * @param context
     */
    public FieldWriteback(ActionContext context) {
        super(context, Boolean.TRUE, 5);
    }

    @Override
    public ActionType getType() {
        return ActionType.FIELDWRITEBACK;
    }

    @Override
    protected void buildTargetRecord(Record record, String dataFilterSql) {
        JSONArray items = ((JSONObject) context.getActionContent()).getJSONArray("items");
        Map<String, String> t2sMap = new HashMap<>();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            String sourceField = item.getString("sourceField");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)
                    || MetadataHelper.getLastJoinField(sourceEntity, sourceField) == null) {
                continue;
            }
            t2sMap.put(targetField, sourceField);
        }

        if (t2sMap.isEmpty()) {
            return;
        }

        String sql = String.format("select %s from %s where %s = ?",
                StringUtils.join(t2sMap.values(), ","), sourceEntity.getName(), followSourceField);

        final Record o = Application.createQueryNoFilter(sql)
                .setParameter(1, targetRecordId)
                .record();
        if (o == null) {
            return;
        }

        for (Map.Entry<String, String> e : t2sMap.entrySet()) {
            Object value = o.getObjectValue(e.getValue());
            // NOTE 忽略空值
            if (value == null) {
                continue;
            }

            Field sourceField = MetadataHelper.getLastJoinField(sourceEntity, e.getValue());
            Field targetField = targetEntity.getField(e.getKey());
            Object newValue = new CompatibleValueConversion(sourceField, targetField).conversion(value);
            if (newValue != null) {
                record.setObjectValue(targetField.getName(), newValue);
            }
        }
    }
}
