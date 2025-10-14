/*!
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild.core.service.trigger.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.InternalPermission;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.trigger.ActionContext;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author devezhao
 * @since 2023/9/27
 */
@Slf4j
public abstract class AutoHoldTriggerAction extends TriggerAction {

    private Set<ID> willRecords;

    protected AutoHoldTriggerAction(ActionContext actionContext) {
        super(actionContext);
    }

    /**
     * 删除前获取相关资源
     *
     * @param operatingContext
     * @throws TriggerException
     */
    @Override
    protected void prepare(OperatingContext operatingContext) throws TriggerException {
        if (operatingContext.getAction() == InternalPermission.DELETE_BEFORE) {
            willRecords = getRelatedRecords42(actionContext, operatingContext.getFixedRecordId());
        }
    }

    /**
     * 获取待处理记录
     *
     * @param operatingContext
     * @return
     */
    protected Set<ID> getWillRecords(OperatingContext operatingContext) {
        if (willRecords == null) {
            willRecords = getRelatedRecords42(actionContext, operatingContext.getFixedRecordId());
        }
        return willRecords;
    }

    /**
     * 获取相关记录
     *
     * @param actionContext
     * @param sourceRecordId
     * @return
     */
    protected Set<ID> getRelatedRecords42(ActionContext actionContext, ID sourceRecordId) {
        // 共享的需要使用记录 ID
        if (sourceRecordId.getEntityCode() == EntityHelper.ShareAccess) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(sourceRecordId, "recordId");
            if (o == null) return Collections.emptySet();
            sourceRecordId = (ID) o[0];
        }

        final JSONObject actionContent = (JSONObject) actionContext.getActionContent();

        Entity sourceEntity = actionContext.getSourceEntity();
        JSONArray fields = actionContent.getJSONArray("fields");
        if (fields == null) fields = actionContent.getJSONArray("fields42");
        if (fields == null) fields = actionContent.getJSONArray("revokeFields");

        List<String> fieldsRefs = new ArrayList<>();
        List<Field> fieldsRelateds = new ArrayList<>();

        for (Object o : fields) {
            String field = (String) o;
            // 自己
            if (SOURCE_SELF.equals(field)) {
                fieldsRefs.add(sourceEntity.getPrimaryField().getName());
            } else if (field.contains(".")) {
                String[] fieldAndEntity = field.split("\\.");
                if (MetadataHelper.containsField(fieldAndEntity[1], fieldAndEntity[0])) {
                    fieldsRelateds.add(MetadataHelper.getField(fieldAndEntity[1], fieldAndEntity[0]));
                }
            } else {
                fieldsRefs.add(field);
            }
        }

        final Set<ID> relateds = new HashSet<>();

        // 引用项
        if (!fieldsRefs.isEmpty()) {
            fieldsRefs.add(sourceEntity.getPrimaryField().getName());
            Object[] o = Application.getQueryFactory().uniqueNoFilter(sourceRecordId, fieldsRefs.toArray(new String[0]));
            if (o != null) {
                for (int i = 0; i < o.length - 1; i++) {
                    Object id = o[i];
                    if (id != null) {
                        if (id instanceof ID[]) Collections.addAll(relateds, (ID[]) id);
                        else relateds.add((ID) id);
                    }
                }
            }
        }

        // 相关项
        for (Field field : fieldsRelateds) {
            Entity oe = field.getOwnEntity();
            String sql = String.format("select %s from %s where ", oe.getPrimaryField().getName(), oe.getName());

            // N2N
            if (field.getType() == FieldType.REFERENCE_LIST) {
                sql += String.format(
                        "exists (select recordId from NreferenceItem where ^%s = recordId and belongField = '%s' and referenceId = '%s')",
                        oe.getPrimaryField().getName(), field.getName(), sourceRecordId);
            } else {
                sql += String.format("%s = '%s'", field.getName(), sourceRecordId);
            }

            Object[][] array = Application.createQueryNoFilter(sql).array();
            for (Object[] o : array) {
                relateds.add((ID) o[0]);
            }
        }

        return relateds;
    }

    /**
     * 获取审批状态
     *
     * @param recordId
     * @return
     * @see ApprovalHelper#getApprovalState(ID)
     */
    protected ApprovalState getApprovalState(ID recordId) {
        if (MetadataHelper.hasApprovalField(MetadataHelper.getEntity(recordId.getEntityCode()))) {
            try {
                return ApprovalHelper.getApprovalState(recordId);
            } catch (NoRecordFoundException ignored) {}
        }
        return null;
    }
}
