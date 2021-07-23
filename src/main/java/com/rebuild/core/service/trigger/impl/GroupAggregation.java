/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.MissingMetaExcetion;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.TriggerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 分组聚合
 *
 * @author devezhao
 * @since 2021/6/28
 */
@Slf4j
public class GroupAggregation extends FieldAggregation {

    public GroupAggregation(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.GROUPAGGREGATION;
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        if (sourceEntity != null) return;  // 已经初始化

        final JSONObject actionContent = (JSONObject) context.getActionContent();

        sourceEntity = context.getSourceEntity();
        targetEntity = MetadataHelper.getEntity(actionContent.getString("targetEntity"));

        // 分组字段关联 <Source, Target>

        Map<String, String> groupFieldsMapping = new HashMap<>();
        for (Object o : actionContent.getJSONArray("groupFields")) {
            JSONObject item = (JSONObject) o;
            String sourceField = item.getString("sourceField");
            String targetField = item.getString("targetField");

            if (!sourceEntity.containsField(sourceField)) {
                throw new MissingMetaExcetion(sourceField, sourceEntity.getName());
            }
            if (!targetEntity.containsField(targetField)) {
                throw new MissingMetaExcetion(targetField, targetEntity.getName());
            }
            groupFieldsMapping.put(sourceField, targetField);
        }

        // 源纪录数据

        String ql = String.format("select %s from %s where %s = ?",
                StringUtils.join(groupFieldsMapping.keySet().iterator(), ","),
                sourceEntity.getName(), sourceEntity.getPrimaryField().getName());

        Record sourceRecord = Application.getQueryFactory().createQueryNoFilter(ql)
                .setParameter(1, context.getSourceRecord())
                .record();

        // 找到目标记录数据

        List<String> qFields = new ArrayList<>();
        List<String> qFieldsFollow = new ArrayList<>();
        for (Map.Entry<String, String> e : groupFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();

            Object val = sourceRecord.getObjectValue(sourceField);
            if (val != null) {
                if (val instanceof Date) {
                    val = CalendarUtils.getUTCDateFormat().format(val);
                }

                qFields.add(String.format("%s = '%s'", targetField, val));
                qFieldsFollow.add(String.format("%s = '%s'", sourceField, val));
            }
        }

        this.followSourceWhere = StringUtils.join(qFieldsFollow.iterator(), " and ");

        ql = String.format("select %s from %s where ( %s )",
                targetEntity.getPrimaryField().getName(), targetEntity.getName(),
                StringUtils.join(qFields.iterator(), " and "));

        Object[] targetRecord = Application.getQueryFactory().createQueryNoFilter(ql).unique();
        if (targetRecord != null) {
            targetRecordId = (ID) targetRecord[0];
            return;
        }

        // 是否自动创建记录
        if (!actionContent.getBoolean("autoCreate")) return;

        // 不必担心必填字段，必填只是前端约束
        // 还可以通过设置字段默认值来完成必填字段的自动填写

        Record newTargetRecord = EntityHelper.forNew(targetEntity.getEntityCode(), UserService.SYSTEM_USER);
        for (Map.Entry<String, String> e : groupFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();

            Object val = sourceRecord.getObjectValue(sourceField);
            if (val != null) {
                newTargetRecord.setObjectValue(targetField, val);
            }
        }

        newTargetRecord = Application.getEntityService(targetEntity.getEntityCode()).create(newTargetRecord);
        targetRecordId = newTargetRecord.getPrimary();
    }
}
