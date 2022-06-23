/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.OperationDeniedException;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.DataSpecificationNoRollbackException;
import com.rebuild.core.service.InternalPersistService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

/**
 * 审批流程。此类所有方法不应直接调用，而是通过 ApprovalProcessor
 * <p>
 * isWaiting - 因为会签的关系还不能进入下一步审批，因此需要等待。待会签完毕，此值将更新为 true
 * isCanceled - 是否作废。例如或签中，一人同意其他即作废
 *
 * @author devezhao
 * @since 07/11/2019
 */
@Slf4j
@Service
public class ApprovalStepService extends InternalPersistService {

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
     * @param cc
     * @param nextApprovers
     */
    public void txSubmit(Record recordOfMain, Set<ID> cc, Set<ID> nextApprovers) {
        final ID submitter = UserContextHolder.getUser();
        final ID recordId = recordOfMain.getPrimary();
        final ID approvalId = recordOfMain.getID(EntityHelper.ApprovalId);

        // 使用新流程，作废之前的步骤
        cancelAliveSteps(recordId, null, null, null, false);

        super.update(recordOfMain);

        String entityLabel = EasyMetaFactory.getLabel(recordOfMain.getEntity());

        // 审批人
        String approvalMsg = Language.L("有一条 %s 记录请你审批", entityLabel);

        Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, submitter);
        step.setID("recordId", recordId);
        step.setID("approvalId", approvalId);
        step.setString("node", recordOfMain.getString(EntityHelper.ApprovalStepNode));
        step.setString("prevNode", FlowNode.NODE_ROOT);
        step.setString("nodeBatch", getBatchNo(recordId, approvalId, FlowNode.NODE_ROOT));
        for (ID to : nextApprovers) {
            Record clone = step.clone();
            clone.setID("approver", to);
            super.create(clone);

            Application.getNotifications().send(MessageBuilder.createApproval(to, approvalMsg, recordId));
        }

        // 抄送人
        if (cc != null && !cc.isEmpty()) {
            String ccMsg = Language.L("用户 @%s 提交了一条 %s 审批，请知悉", submitter, entityLabel);
            for (ID to : cc) {
                Application.getNotifications().send(MessageBuilder.createApproval(to, ccMsg, recordId));
            }
        }

        // see #getSubmitter
        String ckey = "ApprovalSubmitter" + recordId + approvalId;
        Application.getCommonsCache().evict(ckey);
    }

    /**
     * @param stepRecord
     * @param signMode
     * @param cc
     * @param nextApprovers [驳回时无需]
     * @param nextNode      下一节点或回退节点
     * @param addedData     [驳回时无需]
     * @param checkUseGroup [驳回时无需]
     */
    public void txApprove(Record stepRecord, String signMode, Set<ID> cc, Set<ID> nextApprovers, String nextNode, Record addedData, String checkUseGroup) {
        // 审批时更新主记录（驳回时不会传这个值）
        if (addedData != null) {
            GeneralEntityServiceContextHolder.setAllowForceUpdate(addedData.getPrimary());
            try {
                Application.getEntityService(addedData.getEntity().getEntityCode()).update(addedData);
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

        ApprovalState state = (ApprovalState) ApprovalState.valueOf(stepRecord.getInt("state"));

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

        String entityLabel = EasyMetaFactory.getLabel(MetadataHelper.getEntity(recordId.getEntityCode()));

        // 抄送人
        if (cc != null && !cc.isEmpty()) {
            String ccMsg = Language.L("用户 @%s 提交的 %s 审批已由 @%s %s，请知悉",
                    submitter, entityLabel, approver, Language.L(state));
            for (ID c : cc) {
                Application.getNotifications().send(MessageBuilder.createApproval(c, ccMsg, recordId));
            }
        }

        // 拒绝了直接返回
        if (state == ApprovalState.REJECTED || state == ApprovalState.BACKED) {
            // 拒绝了，同一节点的其他审批人全部作废
            cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, true);

            // 更新主记录
            Record recordOfMain = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
            if (recordOfMain.getEntity().containsField(EntityHelper.ApprovalLastUser)) {
                recordOfMain.setID(EntityHelper.ApprovalLastUser, approver);
            }

            // 退回
            if (state == ApprovalState.BACKED) {
                recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNode);
                super.update(recordOfMain);

                createBackedNodes(currentNode, nextNode, recordId, approvalId, approver);
            }
            // 驳回
            else {
                recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.REJECTED.getState());
                super.update(recordOfMain);

                String rejectedMsg = Language.L("@%s 驳回了你的 %s 审批，请重新提交", approver, entityLabel);
                Application.getNotifications().send(MessageBuilder.createApproval(submitter, rejectedMsg, recordId));
            }
            return;
        }

        // 或签/会签
        boolean goNextNode = true;

        String approvalMsg = Language.L("有一条 %s 记录请你审批", entityLabel);

        // 或签。一人通过其他作废
        if (FlowNode.SIGN_OR.equals(signMode)) {
            cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, true);
        }
        // 会签。检查是否都签了
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

                    Application.getNotifications().send(MessageBuilder.createApproval((ID) o[1], approvalMsg, recordId));
                }
            }
        }

        // 最终状态（审批通过）
        if (goNextNode && (nextApprovers == null || nextNode == null)) {
            Application.getEntityService(recordId.getEntityCode()).approve(recordId, ApprovalState.APPROVED, approver);
            return;
        }

        // 进入下一步
        if (goNextNode) {
            Record recordOfMain = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
            recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNode);
            if (recordOfMain.getEntity().containsField(EntityHelper.ApprovalLastUser)) {
                recordOfMain.setID(EntityHelper.ApprovalLastUser, approver);
            }
            super.update(recordOfMain);
        }

        // 下一步审批人
        if (nextApprovers != null) {
            String nodeBatch = getBatchNo(recordId, approvalId, nextNode);
            for (ID to : nextApprovers) {
                ID created = createStepIfNeed(recordId, approvalId, nextNode, to, !goNextNode, currentNode,
                        (Date) stepObject[4], nodeBatch);

                // 非会签通知审批
                if (goNextNode && created != null) {
                    Application.getNotifications().send(MessageBuilder.createApproval(to, approvalMsg, recordId));
                }
            }
        }
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

        if (isRevoke) {
            if (!UserHelper.isAdmin(opUser)) {
                throw new OperationDeniedException(Language.L("仅管理员可撤销审批"));
            }
        } else {
            ID s = ApprovalHelper.getSubmitter(recordId, approvalId);
            if (!opUser.equals(s)) {
                throw new OperationDeniedException(Language.L("仅提交人可撤回审批"));
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
            Application.getEntityService(recordId.getEntityCode()).approve(recordId, ApprovalState.REVOKED, null);
        } else {
            Record recordOfMain = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
            recordOfMain.setInt(EntityHelper.ApprovalState, useState.getState());
            super.update(recordOfMain);
        }
    }

    /**
     * @param recordId
     * @param approvalId
     * @param node
     * @param approver
     * @param isWaiting
     * @param prevNode
     * @param afterCreate
     * @return
     */
    private ID createStepIfNeed(ID recordId, ID approvalId, String node, ID approver, boolean isWaiting, String prevNode, Date afterCreate, String nodeBatch) {
        Object[] hadApprover = Application.createQueryNoFilter(
                "select stepId from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and approver = ? and isCanceled = 'F' and createdOn >= ?")
                .setParameter(1, recordId)
                .setParameter(2, approvalId)
                .setParameter(3, node)
                .setParameter(4, approver)
                .setParameter(5, afterCreate)
                .unique();
        if (hadApprover != null) return null;

        Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, UserContextHolder.getUser());
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

    /**
     * 作废流程步骤
     *
     * @param recordId
     * @param approvalId
     * @param node
     * @param excludeStep
     * @param darftOnly 仅草稿
     */
    private void cancelAliveSteps(ID recordId, ID approvalId, String node, ID excludeStep, boolean darftOnly) {
        String sql = "select stepId from RobotApprovalStep where recordId = ? and isCanceled = 'F'";
        if (approvalId != null) sql += " and approvalId = '" + approvalId + "'";
        if (node != null) sql += " and node = '" + node + "'";
        if (darftOnly) sql += " and state = " + ApprovalState.DRAFT.getState();

        Object[][] canceled = Application.createQueryNoFilter(sql)
                .setParameter(1, recordId)
                .array();

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
     * 审批提交人
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

        // 其他状态不能自动审批
        if (!(currentState == ApprovalState.PROCESSING || currentState == ApprovalState.APPROVED)) {
            
            if (useApprover == null) useApprover = UserService.SYSTEM_USER;
            if (useApproval == null) useApproval = APPROVAL_NOID;

            ID stepId = createStepIfNeed(recordId, useApproval,
                    FlowNode.NODE_AUTOAPPROVAL, useApprover, false, FlowNode.NODE_ROOT,
                    CalendarUtils.now(), getBatchNo(recordId, useApproval, FlowNode.NODE_ROOT));

            Record step = EntityHelper.forUpdate(stepId, useApprover, false);
            step.setInt("state", ApprovalState.APPROVED.getState());
            step.setString("remark", Language.L("自动审批"));
            super.update(step);

            // 更新记录审批状态
            Record recordOfMain = EntityHelper.forUpdate(recordId, UserService.SYSTEM_USER, false);
            recordOfMain.setID(EntityHelper.ApprovalId, useApproval);
            recordOfMain.setString(EntityHelper.ApprovalStepNode, FlowNode.NODE_AUTOAPPROVAL);
            super.update(recordOfMain);

            Application.getEntityService(recordId.getEntityCode()).approve(recordId, ApprovalState.APPROVED, useApprover);
            return true;

        } else {
            log.warn("Invalid state {} for auto approval", currentState);
            return false;
        }
    }

    /**
     * @param rejectNode
     * @param recordId
     * @param approvalId
     */
    private void createBackedNodes(String currentNode, String rejectNode, ID recordId, ID approvalId, ID approver) {
        // 1.最近提交的
        Object[] lastRoot = Application.createQueryNoFilter(
                "select createdOn from RobotApprovalStep where prevNode = 'ROOT' and recordId = ? and approvalId = ? order by createdOn desc")
                .setParameter(1, recordId)
                .setParameter(2, approvalId)
                .unique();

        // 2.上一节点审批
        Object[][] prevNodes = Application.createQueryNoFilter(
                        "select approver,isCanceled,stepId from RobotApprovalStep where node = ? and recordId = ? and approvalId = ? and createdOn >= ?")
                .setParameter(1, rejectNode)
                .setParameter(2, recordId)
                .setParameter(3, approvalId)
                .setParameter(4, lastRoot[0])
                .array();

        String entityLabel = EasyMetaFactory.getLabel(MetadataHelper.getEntity(recordId.getEntityCode()));
        String nodeBatch = getBatchNo(recordId, approvalId, rejectNode);

        // 3.复制回退节点
        for (Object[] o : prevNodes) {
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
                String rejectedMsg = Language.L("@%s 退回了你的 %s 审批，请重新审核", o[0], entityLabel);
                Application.getNotifications().send(MessageBuilder.createApproval((ID) o[0], rejectedMsg, recordId));
            }

            // 标记为退回
            Record backed = EntityHelper.forUpdate((ID) o[2], approver);
            backed.setBoolean("isBacked", true);
            super.update(backed);
        }
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
}
