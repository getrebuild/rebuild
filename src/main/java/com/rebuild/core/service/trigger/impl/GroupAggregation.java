/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Query;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组聚合
 *
 * @author devezhao
 * @since 2021/6/28
 */
@Slf4j
public class GroupAggregation extends FieldAggregation {

    // 目标记录
    private ID targetRecordId;

    public GroupAggregation(ActionContext context) {
        super(context);
    }

    @Override
    public ActionType getType() {
        return ActionType.GROUPAGGREGATION;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
        List<ID> tschain = checkTriggerChain();
        if (tschain == null) return;

        this.prepare(operatingContext);


    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        if (sourceEntity != null) return;  // 已经初始化

        final JSONObject actionContent = (JSONObject) context.getActionContent();

        // FIELD.ENTITY
        String[] targetFieldEntity = actionContent.getString("targetEntity").split("\\.");
        sourceEntity = context.getSourceEntity();
        targetEntity = MetadataHelper.getEntity(targetFieldEntity[1]);

        String followSourceField = targetFieldEntity[0];
        if (!sourceEntity.containsField(followSourceField)) {
            throw new MissingMetaExcetion(followSourceField, sourceEntity.getName());
        }

        // 分组字段映射
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

        // 找到源纪录数据
        String ql = String.format("select %s from %s where %s = ?",
                StringUtils.join(groupFieldsMapping.keySet().iterator(), ","),
                sourceEntity.getName(), sourceEntity.getPrimaryField().getName());

        Record sourceRecord = Application.getQueryFactory().createQueryNoFilter(ql)
                .setParameter(1, context.getSourceRecord())
                .record();

        // 找到目标记录数据
        ql = String.format("select %s from %s where",
                targetEntity.getPrimaryField().getName(), targetEntity.getName());
        Map<Integer, Object> paramsMap = new HashMap<>();
        for (Map.Entry<String, String> e : groupFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();

            Object val = sourceRecord.getObjectValue(sourceField);
            paramsMap.put(paramsMap.size() + 1, val);
            ql += String.format("%s = ? and ", targetField);
        }
        ql = ql.substring(0, ql.length() - 5);
        System.out.println(ql);

        Query query = Application.getQueryFactory().createQueryNoFilter(ql);
        for (Map.Entry<Integer, Object> e : paramsMap.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
        }

        Object[] targetValue = query.unique();
        if (targetValue != null) {
            targetRecordId = (ID) targetValue[0];
            return;
        }

        boolean autoCreate = actionContent.getBoolean("autoCreate");
        if (!autoCreate) return;

        // 自动创建记录
        Record targetRecord = EntityHelper.forNew(targetEntity.getEntityCode(), UserService.SYSTEM_USER);
        for (Map.Entry<String, String> e : groupFieldsMapping.entrySet()) {
            String sourceField = e.getKey();
            String targetField = e.getValue();

            Object val = sourceRecord.getObjectValue(sourceField);
            targetRecord.setObjectValue(targetField, val);
        }

        Application.getEntityService(targetEntity.getEntityCode()).create(targetRecord);
    }
}
