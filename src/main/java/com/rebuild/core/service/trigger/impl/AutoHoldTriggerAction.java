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

    // 删除前保持
    @Override
    protected void prepare(OperatingContext operatingContext) throws TriggerException {
        if (operatingContext.getAction() == InternalPermission.DELETE_BEFORE) {
            willRecords = getRelatedRecords(
                    actionContext, operatingContext.getFixedRecordId());
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
            willRecords = getRelatedRecords(
                    actionContext, operatingContext.getFixedRecordId());
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
    protected static Set<ID> getRelatedRecords(ActionContext actionContext, ID sourceRecordId) {
        // 共享的需要使用记录 ID
        if (sourceRecordId.getEntityCode() == EntityHelper.ShareAccess) {
            Object[] o = Application.getQueryFactory().uniqueNoFilter(sourceRecordId, "recordId");
            if (o == null) return Collections.emptySet();
            sourceRecordId = (ID) o[0];
        }

        final JSONObject actionContent = (JSONObject) actionContext.getActionContent();

        Entity sourceEntity = actionContext.getSourceEntity();
        JSONArray fs = actionContent.getJSONArray("fields");
        // for AutoApproval
        if (fs == null) fs = actionContent.getJSONArray("revokeFields");

        List<String> fieldsSelf = new ArrayList<>();
        List<Field> fieldsRefto = new ArrayList<>();
        boolean hasSelf = false;

        for (Object o : fs) {
            String field = (String) o;
            if (field.contains(".")) {
                String[] fieldAndEntity = field.split("\\.");
                if (MetadataHelper.containsField(fieldAndEntity[1], fieldAndEntity[0])) {
                    fieldsRefto.add(MetadataHelper.getField(fieldAndEntity[1], fieldAndEntity[0]));
                }

            } else {
                if (SOURCE_SELF.equals(field)) {
                    fieldsSelf.add(sourceEntity.getPrimaryField().getName());
                    hasSelf = true;
                } if (sourceEntity.containsField(field)) {
                    fieldsSelf.add(field);
                }
            }
        }

        final Set<ID> relateds = new HashSet<>();

        if (!fieldsSelf.isEmpty()) {
            fieldsSelf.add(sourceEntity.getPrimaryField().getName());
            Object[] o = Application.getQueryFactory().uniqueNoFilter(sourceRecordId, fieldsSelf.toArray(new String[0]));
            if (o != null) {
                for (Object id : o) {
                    if (id != null) {
                        if (id instanceof ID[]) Collections.addAll(relateds, (ID[]) id);
                        else relateds.add((ID) id);
                    }
                }
            }
        }

        for (Field refto : fieldsRefto) {
            final Entity ownEntity = refto.getOwnEntity();
            String sql = String.format("select %s from %s where ",
                    ownEntity.getPrimaryField().getName(), ownEntity.getName());

            // N2N
            if (refto.getType() == FieldType.REFERENCE_LIST) {
                sql += String.format(
                        "exists (select recordId from NreferenceItem where ^%s = recordId and belongField = '%s' and referenceId = '%s')",
                        ownEntity.getPrimaryField().getName(), refto.getName(), sourceRecordId);
            } else {
                sql += String.format("%s = '%s'", refto.getName(), sourceRecordId);
            }

            Object[][] array = Application.createQueryNoFilter(sql).array();
            for (Object[] o : array) relateds.add((ID) o[0]);
        }

        // 不含自己
        if (!hasSelf) relateds.remove(sourceRecordId);

        return relateds;
    }

    /**
     * 获取审批状态
     *
     * @param recordId
     * @return
     * @see ApprovalHelper#getApprovalState(ID)
     */
    protected static ApprovalState getApprovalState(ID recordId) {
        if (MetadataHelper.hasApprovalField(MetadataHelper.getEntity(recordId.getEntityCode()))) {
            try {
                return ApprovalHelper.getApprovalState(recordId);
            } catch (NoRecordFoundException ignored) {
                return null;
            }
        }
        return null;
    }
}
