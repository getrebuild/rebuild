/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerManager;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerWhen;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
     * @param approvalId
     * @param fallbackName
     * @return
     */
    public static String getNodeNameById(String nodeId, ID approvalId, boolean fallbackName) {
        FlowDefinition flowDefinition = RobotApprovalManager.instance.getFlowDefinition(approvalId);
        FlowParser flowParser = flowDefinition.createFlowParser();
        for (FlowNode node : flowParser.getAllNodes()) {
            if (nodeId.equals(node.getNodeId())) {
                String name = node.getNodeName();
                if (StringUtils.isBlank(name) && fallbackName) name = "@" + nodeId;
                return name;
            }
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

    /**
     * 获取超时配置
     *
     * @param approvalId
     * @param node
     * @return
     * @see FlowNode#getExpiresAuto()
     */
    public static JSONObject getExpiresAuto(ID approvalId, String node) {
        FlowNode n = getFlowNode(approvalId, node);
        return n == null ? null : n.getExpiresAuto();
    }

    /**
     * @param approvalId
     * @param node
     * @return
     */
    public static FlowNode getFlowNode(ID approvalId, String node) {
        try {
            return RobotApprovalManager.instance.getFlowDefinition(approvalId)
                    .createFlowParser().getNode(node);
        } catch (ConfigurationException | ApprovalException ignored) {}
        return null;
    }

    /**
     * @param recordId
     * @return
     */
    public static FlowNode getCurrentFlowNode(ID recordId) {
        ApprovalStatus s = getApprovalStatus(recordId);
        return getFlowNode(s.getApprovalId(), s.getCurrentStepNode());
    }

    /**
     * @param recordId
     * @param user
     * @return
     */
    public static boolean isAllowEditableRecord(ID recordId, ID user) {
        // 明细需要使用主记录判断
        if (MetadataHelper.getEntity(recordId.getEntityCode()).getMainEntity() != null) {
            recordId = QueryHelper.getMainIdByDetail(recordId);
        }

        ApprovalStatus s = getApprovalStatus(recordId);
        FlowNode node = getFlowNode(s.getApprovalId(), s.getCurrentStepNode());
        if (node == null || node.getEditableMode() != FlowNode.EDITABLE_MODE_RECORD) return false;

        JSONArray current = new ApprovalProcessor(recordId, s.getApprovalId()).getCurrentStep(s);
        for (Object o : current) {
            JSONObject step = (JSONObject) o;
            if (StringUtils.equalsIgnoreCase(user.toLiteral(), step.getString("approver"))) return true;
        }
        return false;
    }

    /**
     * 获取审批超时时间
     *
     * @param createdOn
     * @param eaConf
     * @param recordId
     * @return
     * @see #getExpiresAuto(ID, String)
     */
    public static Date getExpiresTime(Date createdOn, JSONObject eaConf, ID recordId) {
        final int expiresAuto = eaConf == null ? 0 : eaConf.getIntValue("expiresAuto");

        if (expiresAuto == 1) {
            int auto1Value = Math.max((int) eaConf.getDoubleValue("expiresAuto1Value"), 1);
            String auto1Type = eaConf.getString("expiresAuto1ValueType");
            // I,H,D
            return CalendarUtils.add(createdOn, auto1Value,
                    "I".equals(auto1Type) ? Calendar.MINUTE : "H".equals(auto1Type) ? Calendar.HOUR_OF_DAY : Calendar.DAY_OF_MONTH);
        }
        else if (expiresAuto == 2) {
            String auto2Value = eaConf.getString("expiresAuto2Value");
            Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
            if (!entity.containsField(auto2Value)) {
                log.warn("Invalid field : {} in {}", auto2Value, entity.getName());
                return null;
            }

            return (Date) QueryHelper.queryFieldValue(recordId, auto2Value);
        }
        // 未启用
        return null;
    }

    /**
     * 获取审批超时时间
     *
     * @param createdOn
     * @param eaConf
     * @param recordId
     * @return `null` 表示未开启
     * @see #getExpiresAuto(ID, String)
     */
    public static Long getExpiresTimeLeft(Date createdOn, JSONObject eaConf, ID recordId) {
        Date exp = getExpiresTime(createdOn, eaConf, recordId);
        if (exp == null) return null;
        return (CalendarUtils.now().getTime() - exp.getTime()) / 1000;
    }
}
