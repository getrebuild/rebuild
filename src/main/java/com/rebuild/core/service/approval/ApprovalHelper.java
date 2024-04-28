/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerManager;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerWhen;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2019/10/23
 */
@Slf4j
public class ApprovalHelper {

    // 审批-发起人
    public static final String APPROVAL_SUBMITOR = "$SUBMITOR$.";
    // 审批-审批人
    public static final String APPROVAL_APPROVER = "$APPROVER$.";

    /**
     * 获取提交人
     *
     * @param recordId
     * @return
     */
    public static ID getSubmitter(ID recordId) {
        Object[] approvalId = Application.getQueryFactory().uniqueNoFilter(recordId, EntityHelper.ApprovalId);
        Assert.notNull(approvalId, "Cannot found approval of record : " + recordId);
        return getSubmitter(recordId, (ID) approvalId[0]);
    }

    /**
     * 获取提交人
     *
     * @param recordId
     * @param approvalId
     * @return
     */
    public static ID getSubmitter(ID recordId, ID approvalId) {
        return Application.getBean(ApprovalStepService.class).getSubmitter(recordId, approvalId);
    }

    /**
     * @param recordId
     * @return
     * @throws NoRecordFoundException
     */
    public static ApprovalStatus getApprovalStatus(ID recordId) throws NoRecordFoundException {
        Object[] o = Application.getQueryFactory().uniqueNoFilter(recordId,
                EntityHelper.ApprovalId, EntityHelper.ApprovalId + ".name", EntityHelper.ApprovalState, EntityHelper.ApprovalStepNode);
        if (o == null) {
            throw new NoRecordFoundException(recordId, true);
        }
        return new ApprovalStatus((ID) o[0], (String) o[1], (Integer) o[2], (String) o[3], recordId);
    }

    /**
     * @param recordId
     * @return
     * @throws NoRecordFoundException
     * @see #getApprovalStatus(ID)
     */
    public static ApprovalState getApprovalState(ID recordId) throws NoRecordFoundException {
        return getApprovalStatus(recordId).getCurrentState();
    }

    /**
     * 流程是否正在使用中（处于审批中）
     *
     * @param approvalId
     * @return
     */
    public static int checkInUsed(ID approvalId) {
        return checkUsed(approvalId, ApprovalState.PROCESSING);
    }

    /**
     * 获取流程使用状态
     *
     * @param approvalId
     * @param state
     * @return
     */
    public static int checkUsed(ID approvalId, ApprovalState state) {
        Object[] belongEntity = Application.getQueryFactory().uniqueNoFilter(approvalId, "belongEntity");
        Entity entity = MetadataHelper.getEntity((String) belongEntity[0]);

        String sql = String.format(
                "select count(%s) from %s where approvalId = ? and approvalState = ?",
                entity.getPrimaryField().getName(), entity.getName());
        Object[] inUsed = Application.createQueryNoFilter(sql)
                .setParameter(1, approvalId)
                .setParameter(2, state.getState())
                .unique();

        return inUsed != null ? ObjectUtils.toInt(inUsed[0]) : 0;
    }

    /**
     * 验证虚拟字段
     *
     * @param userField
     * @return
     */
    public static Field checkVirtualField(String userField) {
        if (userField.startsWith(APPROVAL_SUBMITOR) || userField.startsWith(APPROVAL_APPROVER)) {
            String realFields = userField.split("\\$\\.")[1];
            Field lastField = MetadataHelper.getLastJoinField(MetadataHelper.getEntity(EntityHelper.User), realFields);
            if (lastField == null) {
                log.warn("No field of virtual found : {}", userField);
            }
            return lastField;
        }
        return null;
    }

    /**
     * @param entityOrRecord
     * @return
     */
    public static String buildApproveMsg(Object entityOrRecord) {
        Entity be = entityOrRecord instanceof  ID
                ? MetadataHelper.getEntity(((ID) entityOrRecord).getEntityCode())
                : (Entity) entityOrRecord;
        return Language.L("有一条 %s 记录请你审批", EasyMetaFactory.getLabel(be));
    }

    /**
     * 根据节点名称获取编号
     *
     * @param nodeName
     * @return
     */
    public static String getNodeIdByName(String nodeName, ID approvalId) {
        FlowDefinition flowDefinition = RobotApprovalManager.instance.getFlowDefinition(approvalId);
        FlowParser flowParser = flowDefinition.createFlowParser();
        for (FlowNode node : flowParser.getAllNodes()) {
            if (nodeName.equals(node.getNodeName())) return node.getNodeId();
        }
        return null;
    }

    /**
     * 根据节点编号获取名称
     *
     * @param nodeId
     * @return
     */
    public static String getNodeNameById(String nodeId, ID approvalId) {
        FlowDefinition flowDefinition = RobotApprovalManager.instance.getFlowDefinition(approvalId);
        FlowParser flowParser = flowDefinition.createFlowParser();
        for (FlowNode node : flowParser.getAllNodes()) {
            if (nodeId.equals(node.getNodeId())) return node.getNodeName();
        }
        return null;
    }

    /**
     * 获取指定的触发器
     *
     * @param entity
     * @param specType
     * @param when
     * @return
     */
    public static TriggerAction[] getSpecTriggers(Entity entity, ActionType specType, TriggerWhen... when) {
        if (when.length == 0) return new TriggerAction[0];

        TriggerAction[] triggers = RobotTriggerManager.instance.getActions(entity, when);
        if (triggers.length == 0 || specType == null) return triggers;

        List<TriggerAction> specTriggers = new ArrayList<>();
        for (TriggerAction t : triggers) {
            if (t.getType() == specType) specTriggers.add(t);
        }
        return specTriggers.toArray(new TriggerAction[0]);
    }
}
