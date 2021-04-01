/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.record.RecordVisitor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @author devezhao
 * @since 2021/3/30
 */
public class AutoUpdate extends FieldAggregation {

    public AutoUpdate(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOUPDATE;
    }

    @Override
    protected void buildTargetRecord(Record record, String dataFilterSql) {
        final JSONArray items = ((JSONObject) context.getActionContent()).getJSONArray("items");

        Set<String> fieldVars = new HashSet<>();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String updateMode = item.getString("updateMode");
            String sourceAny = item.getString("sourceAny");

            if ("FIELD".equalsIgnoreCase(updateMode)) {
                fieldVars.add(sourceAny);
            } else if ("FORMULA".equalsIgnoreCase(updateMode)) {
                Matcher m = FieldAggregation.PATT_FIELD.matcher(sourceAny);
                while (m.find()) {
                    String[] fieldAndFunc = m.group(1).split("\\$\\$\\$\\$");
                    if (MetadataHelper.getLastJoinField(sourceEntity, fieldAndFunc[0]) != null) {
                        fieldVars.add(fieldAndFunc[0]);
                    }
                }
            }
        }

        // 变量值
        Record useSourceData = null;
        if (!fieldVars.isEmpty()) {
            String sql = String.format("select %s from %s where %s = '%s'",
                    StringUtils.join(fieldVars, ","), sourceEntity.getName(),
                    sourceEntity.getPrimaryField().getName(), context.getSourceRecord());
            useSourceData = Application.createQueryNoFilter(sql).record();
        }

        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String targetField = item.getString("targetField");
            if (!MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
                continue;
            }

            String updateMode = item.getString("updateMode");
            String sourceAny = item.getString("sourceAny");

            // 置空
            if ("VNULL".equalsIgnoreCase(updateMode)) {
                record.setNull(targetField);
            }
            // 固定值
            else if ("VFIXED".equalsIgnoreCase(updateMode)) {
                RecordVisitor.setValueByLiteral(targetField, sourceAny, record);
            }
            // 字段
            else if ("FIELD".equalsIgnoreCase(updateMode)) {
                Assert.notNull(useSourceData, "[useRecordData] is null");

                Field sourceField = MetadataHelper.getLastJoinField(sourceEntity, sourceAny);
                if (sourceField == null) continue;

                Object value = useSourceData.getObjectValue(sourceAny);
                Object newValue = value == null ? null : EasyMetaFactory.valueOf(sourceField)
                        .convertCompatibleValue(value, EasyMetaFactory.valueOf(targetEntity.getField(targetField)));
                if (newValue != null) {
                    record.setObjectValue(targetField, newValue);
                }
            }
            // 公式
            else if ("FORMULA".equalsIgnoreCase(updateMode)) {

            }
        }
    }
}
