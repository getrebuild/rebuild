/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.support.general.FieldValueCompatibleConversion;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据撰写
 *
 * @author devezhao
 * @see AutoFillinManager
 * @since 2020/2/7
 */
public class FieldWriteback extends FieldAggregation {

    private static final String EXPR_SPLIT = "#";

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
        Map<String, String> exprsMap = new HashMap<>();

        JSONArray items = ((JSONObject) context.getActionContent()).getJSONArray("items");
        Map<String, String> t2sMap = new HashMap<>();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            String sourceField = item.getString("sourceField");

            // 公式
            if (sourceField.contains(EXPR_SPLIT)) {
                exprsMap.put(targetField, sourceField.split(EXPR_SPLIT)[1]);
                sourceField = sourceField.split(EXPR_SPLIT)[0];
            }

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
            Object newValue = new FieldValueCompatibleConversion(sourceField, targetField).convert(value, exprsMap.get(e.getKey()));
            if (newValue != null) {
                record.setObjectValue(targetField.getName(), newValue);
            }
        }
    }
}
