/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.RegexUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.OperationDeniedException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.InternalPermission;
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.DataSpecificationNoRollbackException;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.service.trigger.RobotTriggerManager;
import com.rebuild.core.service.trigger.RobotTriggerManual;
import com.rebuild.core.service.trigger.RobotTriggerObserver;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerWhen;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.rebuild.core.privileges.bizz.ZeroEntry.AllowRevokeApproval;

/**
 * 审批流程。此类所有方法不应直接调用，而是通过 ApprovalProcessor 封装类
 * <p>
 * isWaiting - 因为会签的关系还不能进入下一步审批，因此需要等待。待会签完毕，此值将更新为 true
 * isCanceled - 是否作废。例如或签中，一人同意其他即作废
 *
 * @author devezhao
 * @since 07/11/2019
 */
@Slf4j
@Service
public class ApprovalStepService extends BaseService {

    /**
     * 虚拟审批
     */
    public static final ID APPROVAL_NOID = EntityHelper.newUnsavedId(28);

    protected ApprovalStepService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.RobotApprovalStep;
    }

    /**
     * @param recordOfMain
     * @param ccUsers
     * @param ccAccounts
     * @param nextApprovers
     */
    public void txSubmit(Record recordOfMain, Set<ID> ccUsers, Set<String> ccAccounts, Set<ID> nextApprovers) {
        final ID submitter = recordOfMain.getEditor();
        final ID recordId = recordOfMain.getPrimary();
        final ID approvalId = recordOfMain.getID(EntityHelper.ApprovalId);

        // 使用新流程，作废之前的步骤
        cancelAliveSteps(recordId, null, null, null, false);

        setApprovalLastX(recordOfMain, null, null);
        setApprovalStepX37(recordOfMain, nextApprovers);
        super.update(recordOfMain);

        final String approveMsg = ApprovalHelper.buildApproveMsg(recordOfMain.getEntity());

        Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, submitter);
        step.setID("recordId", recordId);
        step.setID("approvalId", approvalId);
        step.setString("node", recordOfMain.getString(EntityHelper.ApprovalStepNode));
        step.setString("prevNode", FlowNode.NODE_ROOT);
        step.setString("nodeBatch", getBatchNo(recordId, approvalId, FlowNode.NODE_ROOT));
        for (ID approver : nextApprovers) {
            Record c = step.clone();
            c.setID("approver", approver);
            super.create(c);

            sendNotification(approver, approveMsg, recordId);
        }

        // 抄送
        String ccMsg = Language.L("@%s 提交了一条 %s 审批，请知悉",
                submitter, EasyMetaFactory.getLabel(recordOfMain.getEntity()));
        sendCcMsgs(recordId, ccMsg, ccUsers, ccAccounts);

        // see #getSubmitter
        String ckey = "ApprovalSubmitter" + recordId + approvalId;
        Application.getCommonsCache().evict(ckey);

        execTriggersWhenSR(recordOfMain, TriggerWhen.SUBMIT);
        this.execSopSteps38(recordOfMain);
    }

    /**
     * 审批/驳回
     * @param stepRecord
     * @param signMode
     * @param ccUsers
     * @param ccAccounts
     * @param nextApprovers [驳回时无需]
     * @param nextNode 下一节点或回退节点
     * @param addedData [驳回时无需]
     * @param checkUseGroup [驳回时无需]
     */
    public void txApprove(Record stepRecord, String signMode, Set<ID> ccUsers, Set<String> ccAccounts, Set<ID> nextApprovers, String nextNode, Record addedData, String checkUseGroup) {
        // 审批时更新主记录。驳回时不会/不要传这个值
        if (addedData != null) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(addedData.getPrimary());
            try {
                Application.getEntityService(addedData.getEntity().getEntityCode()).createOrUpdate(addedData);
            } finally {
                GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
            }

            // 检查数据修改后的步骤对不对 GitHub#208
            if (checkUseGroup != null) {
                Object[] stepObject = Application.createQueryNoFilter(
                        "select recordId,approvalId from RobotApprovalStep where stepId = ?")
                        .setParameter(1, stepRecord.getPrimary())
                        .unique();

                ApprovalProcessor approvalProcessor = new ApprovalProcessor((ID) stepObject[0], (ID) stepObject[1]);
                FlowNodeGroup nextNodes = approvalProcessor.getNextNodes();
                if (!nextNodes.getGroupId().equals(checkUseGroup)) {
                    throw new DataSpecificationNoRollbackException(Language.L("由于更改数据导致流程变化，你需要重新审批"));
                }
            }
        }

        final ApprovalState state = (ApprovalState) ApprovalState.valueOf(stepRecord.getInt("state"));

        if (CommonsUtils.hasLength(ccUsers)) {
            stepRecord.setIDArray("ccUsers", ccUsers.toArray(new ID[0]));
        }
        if (CommonsUtils.hasLength(ccAccounts)) {
            stepRecord.setString("ccAccounts", StringUtils.join(ccAccounts, ","));
        }

        super.update(stepRecord);
        final ID stepRecordId = stepRecord.getPrimary();

        final Object[] stepObject = Application.createQueryNoFilter(
                "select recordId,approvalId,node,prevNode,createdOn from RobotApprovalStep where stepId = ?")
                .setParameter(1, stepRecordId)
                .unique();
        final ID submitter = getSubmitter((ID) stepObject[0], (ID) stepObject[1]);
        final ID recordId = (ID) stepObject[0];
        final ID approvalId = (ID) stepObject[1];
        final String currentNode = (String) stepObject[2];
        final ID approver = UserContextHolder.getUser();
        final String remark = stepRecord.getString("remark");

        final Entity entityMeta = MetadataHelper.getEntity(recordId.getEntityCode());
        final String entityLabel = EasyMetaFactory.getLabel(entityMeta);

        // 抄送
        String ccMsg = Language.L("@%s 提交的 %s 审批已由 @%s %s，请知悉",
                submitter, entityLabel, approver, Language.L(state));
        if (StringUtils.isNotBlank(remark)) ccMsg += "\n > " + remark;
        sendCcMsgs(recordId, ccMsg, ccUsers, ccAccounts);

        // 更新主记录
        final Record recordOfMain = EntityHelper.forUpdate(recordId, approver, false);
        setApprovalLastX(recordOfMain, approver, remark);

        // 拒绝了直接返回
        if (state == ApprovalState.REJECTED || state == ApprovalState.BACKED) {
            // 拒绝了，同一节点的其他审批人全部作废
            cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, true);

            // 退回
            if (state == ApprovalState.BACKED) {
                ID[] backedApprovers = createBackedNodes(currentNode, nextNode, recordId, approvalId, approver, remark);

                recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNode);
                setApprovalStepX37(recordOfMain, backedApprovers);
                super.update(recordOfMain);
            }
            // 驳回
            else {
                recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.REJECTED.getState());
                recordOfMain.setNull(EntityHelper.ApprovalStepNode);
                setApprovalStepX37(recordOfMain, null);
                super.update(recordOfMain);

                String rejectedMsg = Language.L("@%s 驳回了你的 %s 审批，请重新提交", approver, entityLabel);
                if (StringUtils.isNotBlank(remark)) rejectedMsg += "\n > " + remark;
                sendNotification(submitter, rejectedMsg, recordId);

                execTriggersWhenSR(recordOfMain, TriggerWhen.REJECTED);
            }
            return;
        }

        // 或签/会签
        boolean goNextNode = true;

        final String approveMsg = ApprovalHelper.buildApproveMsg(entityMeta);

        // 或签:一人通过其他作废
        if (FlowNode.SIGN_OR.equals(signMode)) {
            cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, true);
        }
        // 会签:检查是否都签了
        else {
            Object[][] currentNodeApprovers = Application.createQueryNoFilter(
                    "select state,isWaiting,stepId from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and isCanceled = 'F'")
                    .setParameter(1, recordId)
                    .setParameter(2, approvalId)
                    .setParameter(3, currentNode)
                    .array();
            for (Object[] o : currentNodeApprovers) {
                if ((Integer) o[0] == ApprovalState.DRAFT.getState()) {
                    goNextNode = false;
                    break;
                }
            }

            // 更新下一步审批人可以开始了（若有）
            if (goNextNode && nextNode != null) {
                Object[][] nextNodeApprovers = Application.createQueryNoFilter(
                        "select stepId,approver from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and isWaiting = 'T'")
                        .setParameter(1, recordId)
                        .setParameter(2, approvalId)
                        .setParameter(3, nextNode)
                        .array();
                for (Object[] o : nextNodeApprovers) {
                    Record r = EntityHelper.forUpdate((ID) o[0], approver);
                    r.setBoolean("isWaiting", false);
                    super.update(r);

                    sendNotification((ID) o[1], approveMsg, recordId);
                }
            }
        }

        // 最终状态（审批通过）
        if (goNextNode && (nextApprovers == null || nextNode == null)) {
            recordOfMain.setNull(EntityHelper.ApprovalStepNode);
            setApprovalStepX37(recordOfMain, null);
            super.update(recordOfMain);

            String approvedMsg = Language.L("你提交的 %s 已审批通过", entityLabel);
            if (StringUtils.isNotBlank(remark)) approvedMsg += "\n > " + remark;
            sendNotification(submitter, approvedMsg, recordId);

            Application.getEntityService(recordId.getEntityCode()).approve(recordId, ApprovalState.APPROVED, approver);
            execSopSteps38(recordOfMain);
            return;
        }

        // 进入下一步
        if (goNextNode) {
            recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNode);
            setApprovalStepX37(recordOfMain, nextApprovers);
            super.update(recordOfMain);

            // v3.7 触发器
            execTriggersWhenNodeApproved(recordOfMain, approvalId, currentNode);
        }

        // 下一步审批人
        if (nextApprovers != null) {
            String nodeBatch = getBatchNo(recordId, approvalId, nextNode);
            for (ID to : nextApprovers) {
                ID created = createStepIfNeed(
                        recordId, approvalId, nextNode, to, !goNextNode, currentNode, (Date) stepObject[4], nodeBatch);

                // 非会签通知审批
                if (goNextNode && created != null) {
                    sendNotification(to, approveMsg, recordId);
                }
            }
        }

        execSopSteps38(recordOfMain);
    }

    /**
     * 撤回/撤销
     *
     * @param recordId
     * @param approvalId
     * @param currentNode
     * @param isRevoke 是否为撤销（仅针对审批完成的）
     */
    public void txCancel(ID recordId, ID approvalId, String currentNode, boolean isRevoke) {
        final ID opUser = UserContextHolder.getUser();
        final ApprovalState useState = isRevoke ? ApprovalState.REVOKED : ApprovalState.CANCELED;

        final boolean isAdmin = UserHelper.isAdmin(opUser);

        if (isRevoke) {
            boolean canRevoke = Application.getPrivilegesManager().allow(opUser, AllowRevokeApproval);
            if (!(isAdmin || canRevoke)) {
                throw new OperationDeniedException(Language.L("你无权撤销审批"));
            }
        } else {
            boolean canRevoke = Application.getPrivilegesManager().allow(opUser, AllowRevokeApproval);
            ID s = ApprovalHelper.getSubmitter(recordId, approvalId);
            if (!(canRevoke || opUser.equals(s))) {
                throw new OperationDeniedException(Language.L("你无权撤回审批"));
            }
        }

        Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, opUser);
        step.setID("recordId", recordId);
        step.setID("approvalId", approvalId == null ? APPROVAL_NOID : approvalId);
        step.setID("approver", opUser);
        step.setInt("state", useState.getState());
        step.setString("node", isRevoke ? FlowNode.NODE_REVOKED : FlowNode.NODE_CANCELED);
        step.setString("prevNode", currentNode);
        super.create(step);

        // 撤销
        if (isRevoke) {
            Application.getEntityService(recordId.getEntityCode()).approve(recordId, ApprovalState.REVOKED, opUser);
        } else {
            Record recordOfMain = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
            recordOfMain.setInt(EntityHelper.ApprovalState, useState.getState());
            recordOfMain.setNull(EntityHelper.ApprovalStepNode);
            setApprovalStepX37(recordOfMain, null);
            super.update(recordOfMain);

            execTriggersWhenSR(recordOfMain, TriggerWhen.REJECTED);
        }
    }

    // 创建审批步骤
    private ID createStepIfNeed(ID recordId, ID approvalId, String node, ID approver, boolean isWaiting, String prevNode, Date afterCreate, String nodeBatch) {
        Object[] hasApprover = Application.createQueryNoFilter(
                "select stepId from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and approver = ? and isCanceled = 'F' and createdOn >= ?")
                .setParameter(1, recordId)
                .setParameter(2, approvalId)
                .setParameter(3, node)
                .setParameter(4, approver)
                .setParameter(5, afterCreate)
                .unique();
        if (hasApprover != null) return null;

        Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, approver);
        step.setID("recordId", recordId);
        step.setID("approvalId", approvalId);
        step.setString("node", node);
        step.setID("approver", approver);
        if (isWaiting) step.setBoolean("isWaiting", true);
        if (prevNode != null) step.setString("prevNode", prevNode);
        if (nodeBatch != null) step.setString("nodeBatch", nodeBatch);

        step = super.create(step);
        return step.getPrimary();
    }

    // 作废流程步骤
    private void cancelAliveSteps(ID recordId, ID approvalId, String node, ID excludeStep, boolean darftOnly) {
        String sql = "select stepId from RobotApprovalStep where recordId = ? and isCanceled = 'F'";
        if (approvalId != null) sql += " and approvalId = '" + approvalId + "'";
        if (node != null) sql += " and node = '" + node + "'";
        if (darftOnly) sql += " and state = " + ApprovalState.DRAFT.getState();

        Object[][] canceled = Application.createQueryNoFilter(sql).setParameter(1, recordId).array();

        for (Object[] o : canceled) {
            if (excludeStep != null && excludeStep.equals(o[0])) {
                continue;
            }

            Record step = EntityHelper.forUpdate((ID) o[0], UserContextHolder.getUser());
            step.setBoolean("isCanceled", true);
            super.update(step);
        }
    }

    /**
     * 获取审批的提交人
     *
     * @param recordId
     * @param approvalId
     * @return
     */
    public ID getSubmitter(ID recordId, ID approvalId) {
        final String ckey = "ApprovalSubmitter" + recordId + approvalId;
        ID submitter = (ID) Application.getCommonsCache().getx(ckey);
        if (submitter != null) {
            return submitter;
        }

        // 第一个创建步骤的人为提交人
        Object[] firstStep = Application.createQueryNoFilter(
                "select createdBy from RobotApprovalStep where recordId = ? and approvalId = ? and isCanceled = 'F' order by createdOn asc")
                .setParameter(1, recordId)
                .setParameter(2, approvalId)
                .unique();

        submitter = (ID) firstStep[0];
        Application.getCommonsCache().putx(ckey, submitter);
        return submitter;
    }

    /**
     * 自动审批/一键审批
     *
     * @param recordId
     * @param useApprover
     * @param useApproval
     * @return
     */
    public boolean txAutoApproved(ID recordId, ID useApprover, ID useApproval) {
        final ApprovalState currentState = ApprovalHelper.getApprovalState(recordId);
        if (currentState == ApprovalState.PROCESSING || currentState == ApprovalState.APPROVED) {
            log.warn("Invalid state {} for auto approve : {}", currentState, recordId);
            return false;
        }

        if (useApproval == null) useApproval = APPROVAL_NOID;

        // 作废之前的
        cancelAliveSteps(recordId, null, null, null, false);

        ID stepId = createStepIfNeed(recordId, useApproval, FlowNode.NODE_AUTOAPPROVAL, useApprover,
                Boolean.FALSE, FlowNode.NODE_ROOT, CalendarUtils.now(), getBatchNo(recordId, useApproval, FlowNode.NODE_ROOT));

        Record step = EntityHelper.forUpdate(stepId, useApprover, false);
        step.setInt("state", ApprovalState.APPROVED.getState());
        step.setString("remark", Language.L("自动审批"));
        super.update(step);

        // 更新记录审批状态
        Record recordOfMain = EntityHelper.forUpdate(recordId, useApprover, false);
        recordOfMain.setID(EntityHelper.ApprovalId, useApproval);
        recordOfMain.setString(EntityHelper.ApprovalStepNode, FlowNode.NODE_AUTOAPPROVAL);
        setApprovalStepX37(recordOfMain, null);
        setApprovalLastX(recordOfMain, useApprover, Language.L("自动审批"));
        super.update(recordOfMain);

        // NOTE 自动审批不会给提交人发通知

        Application.getEntityService(recordId.getEntityCode()).approve(recordId, ApprovalState.APPROVED, useApprover);
        return true;
    }

    /**
     * 自动提交
     *
     * @param recordId
     * @param useApprover
     * @param useApproval
     * @see ApprovalProcessor#submit(JSONObject)
     */
    public void txAutoSubmit(ID recordId, ID useApprover, ID useApproval) {
        final ApprovalState currentState = ApprovalHelper.getApprovalState(recordId);
        if (currentState == ApprovalState.PROCESSING || currentState == ApprovalState.APPROVED) {
            log.warn("Invalid state {} for auto submit : {}", currentState, recordId);
            return;
        }

        ApprovalProcessor approvalProcessor = new ApprovalProcessor(recordId, useApproval);
        FlowNodeGroup nextNodes = approvalProcessor.getNextNodes();

        Set<ID> approverList = nextNodes.getApproveUsers(useApprover, recordId, null);
        if (approverList.isEmpty()) {
            throw new ConfigurationException(Language.L("选择的审批流程至少配置一个审批人"));
        }

        Set<ID> ccUsers = nextNodes.getCcUsers(useApprover, recordId, null);
        Set<String> ccAccounts = nextNodes.getCcAccounts(recordId);

        Record recordOfMain = EntityHelper.forUpdate(recordId, useApprover, false);
        recordOfMain.setID(EntityHelper.ApprovalId, useApproval);
        recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
        recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNodes.getApprovalNode().getNodeId());
        txSubmit(recordOfMain, ccUsers, ccAccounts, approverList);

        // 共享的
        Set<ID> ccs4share = nextNodes.getCcUsers4Share(useApprover, recordId, null);
        ApprovalProcessor.share2CcIfNeed(recordId, ccs4share);
    }

    /**
     * 转审
     *
     * @param sourceStepId
     * @param approver
     * @return
     */
    public boolean txReferral(ID sourceStepId, ID approver) {
        final Record sourceStep = QueryHelper.recordNoFilter(sourceStepId);
        final ID recordId = sourceStep.getID("recordId");
        final ID oldApprover = sourceStep.getID("approver");

        // 标记转审
        String attrMore = sourceStep.getString("attrMore");
        JSONObject attrMoreJson = JSONUtils.wellFormat(attrMore) ? JSON.parseObject(attrMore) : new JSONObject();
        attrMoreJson.put("referralFrom", oldApprover);

        Record sourceStepUpdate = EntityHelper.forUpdate(sourceStepId, approver);
        sourceStepUpdate.setString("attrMore", attrMoreJson.toJSONString());
        sourceStepUpdate.setID("approver", approver);
        sourceStepUpdate.setDate(EntityHelper.CreatedOn, CalendarUtils.now());  // 使用当前时间
        super.update(sourceStepUpdate);

        String approveMsg = ApprovalHelper.buildApproveMsg(recordId);
        approveMsg += "\n > " + Language.L("由 %s 转审给你", UserHelper.getName(oldApprover));
        sendNotification(approver, approveMsg, recordId);
        return true;
    }

    /**
     * 加签
     *
     * @param sourceStepId
     * @param approvers
     * @return
     */
    public int txCountersign(ID sourceStepId, ID[] approvers) {
        final Record sourceStep = QueryHelper.recordNoFilter(sourceStepId);
        final ID approver = sourceStep.getID("approver");
        final ID recordId = sourceStep.getID("recordId");
        final ID approvalId = sourceStep.getID("approvalId");
        final String node = sourceStep.getString("node");
        final String prevNode = sourceStep.getString("prevNode");
        final String nodeBatch = sourceStep.getString("nodeBatch");

        String approveMsg = ApprovalHelper.buildApproveMsg(recordId);
        approveMsg += "\n > " + Language.L("由 %s 加签给你", UserHelper.getName(approver));

        final Date fakeDate = CalendarUtils.parse("2019-01-31");

        int c = 0;
        for (ID to : approvers) {
            if (to.equals(approver)) continue;
            ID created = createStepIfNeed(recordId, approvalId, node, to, Boolean.FALSE, prevNode, fakeDate, nodeBatch);

            if (created != null) {
                // 标记加签
                Record newStepUpdate = EntityHelper.forUpdate(created, UserService.SYSTEM_USER);
                String attrMore = String.format("{countersignFrom:'%s'}", approver);
                newStepUpdate.setString("attrMore", attrMore);
                super.update(newStepUpdate);

                sendNotification(to, approveMsg, recordId);
                c++;
            }
        }
        return c;
    }

    // 创建回退节点
    private ID[] createBackedNodes(String currentNode, String rejectNode, ID recordId, ID approvalId, ID approver, String remark) {
        // 1.最近提交的
        Object[] lastRoot = Application.createQueryNoFilter(
                "select createdOn from RobotApprovalStep where prevNode = 'ROOT' and recordId = ? and approvalId = ? order by createdOn desc")
                .setParameter(1, recordId)
                .setParameter(2, approvalId)
                .unique();

        // 2.获取回退到的节点备用
        Object[][] prevNodes = Application.createQueryNoFilter(
                "select approver,isCanceled,stepId from RobotApprovalStep where node = ? and recordId = ? and approvalId = ? and createdOn >= ? order by createdOn desc")
                .setParameter(1, rejectNode)
                .setParameter(2, recordId)
                .setParameter(3, approvalId)
                .setParameter(4, lastRoot[0])
                .array();

        String entityLabel = EasyMetaFactory.getLabel(MetadataHelper.getEntity(recordId.getEntityCode()));
        String nodeBatch = getBatchNo(recordId, approvalId, rejectNode);

        Set<ID> uniqueApprovers = new HashSet<>();

        // 3.复制退回到的节点
        for (Object[] o : prevNodes) {
            // 多次退回会有多个节点，这里通过审批人排重
            if (uniqueApprovers.contains((ID) o[0])) continue;
            uniqueApprovers.add((ID) o[0]);

            Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, approver);
            step.setString("node", rejectNode);
            step.setString("prevNode", currentNode);
            step.setID("recordId", recordId);
            step.setID("approvalId", approvalId);
            step.setID("approver", (ID) o[0]);
            step.setString("nodeBatch", nodeBatch);
            super.create(step);

            // 通知退回
            if (!(Boolean) o[1]) {
                String backedMsg = Language.L("@%s 退回了你的 %s 审批，请重新审批", o[0], entityLabel);
                if (StringUtils.isNotBlank(remark)) backedMsg += "\n > " + remark;
                sendNotification((ID) o[0], backedMsg, recordId);
            }

            // 标记为退回
            Record backed = EntityHelper.forUpdate((ID) o[2], approver);
            backed.setBoolean("isBacked", true);
            super.update(backed);
        }
        return uniqueApprovers.toArray(new ID[0]);
    }

    private String getBatchNo(ID recordId, ID approvalId, String node) {
        Object[] lastNode = Application.getQueryFactory().createQueryNoFilter(
                "select node,nodeBatch from RobotApprovalStep where recordId = ? and approvalId = ? order by createdOn desc")
                .setParameter(1, recordId)
                .setParameter(2, approvalId)
                .unique();
        if (lastNode != null && lastNode[0].equals(node)) {
            return (String) lastNode[1];
        }

        if (lastNode != null && lastNode[1] != null) {
            String index = ((String) lastNode[1]).split("-")[1];
            return String.format("%s-%d", node, ObjectUtils.toInt(index) + 1);
        }

        Object[] o = Application.getQueryFactory().createQueryNoFilter(
                "select count(distinct stepId) from RobotApprovalStep where recordId = ?")
                .setParameter(1, recordId)
                .unique();
        return String.format("%s-%d", node, (Long) o[0]);
    }

    // Clear or set lastXXX
    private void setApprovalLastX(Record record, ID approver, String remark) {
        Entity entity = record.getEntity();

        if (entity.containsField(EntityHelper.ApprovalLastUser)) {
            if (approver == null) record.setNull(EntityHelper.ApprovalLastUser);
            else record.setID(EntityHelper.ApprovalLastUser, approver);
        }

        if (entity.containsField(EntityHelper.ApprovalLastTime)) {
            if (approver == null) record.setNull(EntityHelper.ApprovalLastTime);
            else record.setDate(EntityHelper.ApprovalLastTime, CalendarUtils.now());
        }

        if (entity.containsField(EntityHelper.ApprovalLastRemark)) {
            if (remark == null) record.setNull(EntityHelper.ApprovalLastRemark);
            else record.setString(EntityHelper.ApprovalLastRemark, remark);
        }
    }

    // Clear or set stepXXX
    private void setApprovalStepX37(Record record, Object nextApprovers) {
        if (!record.getEntity().containsField(EntityHelper.ApprovalStepUsers)) return;

        ID[] nextApproversFix = null;
        if (nextApprovers instanceof ID[]) {
            nextApproversFix = (ID[]) nextApprovers;
        } else if (nextApprovers instanceof Collection) {
            //noinspection unchecked
            nextApproversFix = ((Collection<ID>) nextApprovers).toArray(new ID[0]);
        }

        // clear
        if (nextApproversFix == null) {
            // NOTE 保留不清空???
            record.setNull(EntityHelper.ApprovalStepUsers);
            // v3.9 保留审批步骤
//            record.setNull(EntityHelper.ApprovalStepNodeName);
            return;
        }

        // v3.7-1
        record.setIDArray(EntityHelper.ApprovalStepUsers, nextApproversFix);

        // v3.7-2
        String approvalStepNode = record.getString(EntityHelper.ApprovalStepNode);
        ID approvalId = record.getID(EntityHelper.ApprovalId);
        if (approvalStepNode != null && approvalId == null) {
            approvalId = (ID) QueryHelper.queryFieldValue(record.getPrimary(), EntityHelper.ApprovalId);
        }

        if (approvalId == null || approvalStepNode == null) {
            log.warn("No [approvalId] or [approvalStepNode] value found : {}", record.getPrimary());
        } else {
            String name = ApprovalHelper.getNodeNameById(approvalStepNode, approvalId, true);
            record.setString(EntityHelper.ApprovalStepNodeName, name);
        }
    }

    // 抄送
    private void sendCcMsgs(ID recordId, String ccMsg, Set<ID> ccUsers, Set<String> ccAccounts) {
        if (CommonsUtils.hasLength(ccUsers)) {
            for (ID cc : ccUsers) {
                sendNotification(cc, ccMsg, recordId);
            }
        }

        // v3.2 外部人员
        if (!CommonsUtils.hasLength(ccAccounts)) {
            String mobileMsg = MessageBuilder.formatMessage(ccMsg, Boolean.FALSE);
            String emailSubject = Language.L("审批通知");
            String emailMsg = MessageBuilder.formatMessage(ccMsg, Boolean.TRUE);

            for (String me : ccAccounts) {
                if (SMSender.availableSMS() && RegexUtils.isCNMobile(me)) SMSender.sendSMSAsync(me, mobileMsg);
                if (SMSender.availableMail() && RegexUtils.isEMail(me)) SMSender.sendMailAsync(me, emailSubject, emailMsg);
            }
        }
    }

    // 发送通知
    private void sendNotification(ID to, String message, ID recordId) {
        Application.getNotifications().send(MessageBuilder.createApproval(to, message, recordId));
    }

    /**
     * @param approvalRecord
     * @param when
     * @see com.rebuild.core.service.general.GeneralEntityService#approve(ID, ApprovalState, ID)
     */
    private void execTriggersWhenSR(Record approvalRecord, TriggerWhen when) {
        final RobotTriggerManual triggerManual = new RobotTriggerManual();
        final ID approvalUser = UserService.SYSTEM_USER;

        // 传导给明细（若有）
        for (Entity de : approvalRecord.getEntity().getDetialEntities()) {
            TriggerAction[] deHasTriggers = RobotTriggerManager.instance.getActions(de, when);
            if (deHasTriggers.length > 0) {
                for (ID did : QueryHelper.detailIdsNoFilter(approvalRecord.getPrimary(), de)) {
                    Record dAfter = EntityHelper.forUpdate(did, approvalUser, false);
                    if (when == TriggerWhen.SUBMIT) {
                        triggerManual.onSubmit(
                                OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, dAfter));
                    } else if (when == TriggerWhen.REJECTED) {
                        triggerManual.onRejectedOrCancel(
                                OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, dAfter));
                    }
                }
            }
        }

        // 本记录
        if (when == TriggerWhen.SUBMIT) {
            triggerManual.onSubmit(
                    OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, approvalRecord));
        } else if (when == TriggerWhen.REJECTED) {
            triggerManual.onRejectedOrCancel(
                    OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, approvalRecord));
        }
    }

    /**
     * @param approvalRecord
     * @param approvalId
     * @param currentNode
     * @see com.rebuild.core.service.general.GeneralEntityService#approve(ID, ApprovalState, ID)
     * @see RobotTriggerObserver#allowWhenApproved(TriggerAction, OperatingContext)
     */
    private void execTriggersWhenNodeApproved(Record approvalRecord, ID approvalId, String currentNode) {
        final RobotTriggerManual triggerManual = new RobotTriggerManual();
        final ID approvalUser = UserService.SYSTEM_USER;

        FlowParser flowParser = RobotApprovalManager.instance
                .getFlowDefinition(approvalRecord.getEntity(), approvalId).createFlowParser();
        FlowNode flowNode = flowParser.getNode(currentNode);
        String specNodeName = flowNode.getNodeName();
        // 未填写名称
        if (StringUtils.isBlank(specNodeName)) specNodeName = "@" + flowNode.getNodeId();

        List<ID> allowTriggers = new ArrayList<>();

        try {
            // 传导给明细（若有）
            for (Entity de : approvalRecord.getEntity().getDetialEntities()) {
                TriggerAction[] deHasTriggers = RobotTriggerManager.instance.getActions(de, TriggerWhen.APPROVED);
                for (TriggerAction t : deHasTriggers) {
                    if (isSpecApproveNode(t, specNodeName)) allowTriggers.add(t.getActionContext().getConfigId());
                }

                if (!allowTriggers.isEmpty()) {
                    RobotTriggerObserver.setAllowTriggersOnNodeApproved(allowTriggers.toString());
                    for (ID did : QueryHelper.detailIdsNoFilter(approvalRecord.getPrimary(), de)) {
                        Record dAfter = EntityHelper.forUpdate(did, approvalUser, false);
                        triggerManual.onApproved(
                                OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, dAfter));
                    }

                    RobotTriggerObserver.clearAllowTriggersOnNodeApproved();
                    allowTriggers.clear();
                }
            }

            // 本记录
            TriggerAction[] hasTriggers = RobotTriggerManager.instance.getActions(approvalRecord.getEntity(), TriggerWhen.APPROVED);
            for (TriggerAction t : hasTriggers) {
                if (isSpecApproveNode(t, specNodeName)) allowTriggers.add(t.getActionContext().getConfigId());
            }

            if (!allowTriggers.isEmpty()) {
                RobotTriggerObserver.setAllowTriggersOnNodeApproved(allowTriggers.toString());
                triggerManual.onApproved(
                        OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, approvalRecord));
            }

        } finally {
            RobotTriggerObserver.clearAllowTriggersOnNodeApproved();
        }
    }

    private boolean isSpecApproveNode(TriggerAction action, String nodeName) {
        JSONArray whenApproveNodes = ((JSONObject) action.getActionContext().getActionContent())
                .getJSONArray("whenApproveNodes");
        if (whenApproveNodes == null || whenApproveNodes.isEmpty()) return false;
        return whenApproveNodes.contains(nodeName) || whenApproveNodes.contains("*");
    }

    private void execSopSteps38(Record approvalRecord) {
        try {
            CommonsUtils.invokeMethod("com.rebuild.rbv.sop.RobotSopObserver#onApproveManual", approvalRecord);
        } catch (Throwable ignored) {
            log.debug("Unsupportted class [RobotSopObserver]");
        }
    }
}
