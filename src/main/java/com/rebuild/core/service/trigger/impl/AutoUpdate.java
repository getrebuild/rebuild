/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.record.RecordVisitor;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyDateTime;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @author devezhao
 * @since 2021/3/30
 */
public class AutoUpdate extends FieldAggregation {

    private static final String DATE_EXPR = "#";

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
                if (sourceAny.contains(DATE_EXPR)) {
                    fieldVars.add(sourceAny.split(DATE_EXPR)[0]);
                } else {
                    Matcher m = FieldAggregation.PATT_FIELD.matcher(sourceAny);
                    while (m.find()) {
                        String field = m.group(1);
                        if (MetadataHelper.getLastJoinField(sourceEntity, field) != null) {
                            fieldVars.add(field);
                        }
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

            EasyField targetFieldEasy = EasyMetaFactory.valueOf(targetEntity.getField(targetField));

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
                Field sourceField = MetadataHelper.getLastJoinField(sourceEntity, sourceAny);
                if (sourceField == null) continue;

                Object value = Objects.requireNonNull(useSourceData).getObjectValue(sourceAny);
                Object newValue = value == null ? null : EasyMetaFactory.valueOf(sourceField)
                        .convertCompatibleValue(value, targetFieldEasy);
                if (newValue != null) {
                    record.setObjectValue(targetField, newValue);
                }
            }

            // 公式
            else if ("FORMULA".equalsIgnoreCase(updateMode)) {
                Assert.notNull(useSourceData, "[useSourceData] not be null");

                // 日期
                if (sourceAny.contains(DATE_EXPR)) {
                    String fieldName = sourceAny.split(DATE_EXPR)[0];
                    Field sourceField = MetadataHelper.getLastJoinField(sourceEntity, fieldName);
                    if (sourceField == null) continue;

                    Object value = useSourceData.getObjectValue(fieldName);
                    Object newValue = value == null ? null : ((EasyDateTime) EasyMetaFactory.valueOf(sourceField))
                            .convertCompatibleValue(value, targetFieldEasy, sourceAny);
                    if (newValue != null) {
                        record.setObjectValue(targetField, newValue);
                    }
                }

                // 数字
                else {
                    String realFormual = sourceAny.toUpperCase()
                            .replace("×", "*")
                            .replace("÷", "/");
                    for (String fieldName : useSourceData.getAvailableFields()) {
                        String replace = "{" + fieldName.toUpperCase() + "}";
                        if (realFormual.contains(replace)) {
                            Object value = useSourceData.getObjectValue(fieldName);
                            realFormual = realFormual.replace(replace, value == null ? "0" : value.toString());
                        }
                    }

                    Object newValue = AggregationEvaluator.calc(realFormual);
                    if (newValue != null) {
                        DisplayType dt = targetFieldEasy.getDisplayType();
                        if (dt == DisplayType.NUMBER) {
                            record.setLong(targetField, ObjectUtils.toLong(newValue));
                        } else if (dt == DisplayType.DECIMAL) {
                            record.setDouble(targetField, ObjectUtils.toDouble(newValue));
                        }
                    }
                }
            }
        }
    }
}
