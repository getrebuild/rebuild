/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalHelper;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.approval.FlowNode;
import com.rebuild.server.business.trigger.RobotTriggerManual;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.notification.MessageBuilder;

import java.util.Set;

/**
 * 审批流程。此类所有方法不应直接调用，而是通过 ApprovalProcessor
 *
 * isWaiting - 因为会签的关系还不能进入下一步审批，因此需要等待。待会签完毕，此值将更新为 true
 * isCanceled - 是否作废。例如或签中，一人同意其他即作废
 *
 * @author devezhao
 * @since 07/11/2019
 */
public class ApprovalStepService extends BaseService {

	/**
	 * 虚拟审批
	 */
	public static final ID APPROVAL_NOID = ID.valueOf("028-0000000000000000");

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
		final ID submitter = Application.getCurrentUser();
		final ID recordId = recordOfMain.getPrimary();
		final ID approvalId = recordOfMain.getID(EntityHelper.ApprovalId);
		
		// 使用新流程，作废之前的步骤
		cancelAliveSteps(recordId, null, null, null, false);

		super.update(recordOfMain);
		
		String entityLabel = EasyMeta.getLabel(recordOfMain.getEntity());
		String approvalMsg = String.format("有一条%s记录请你审批 @%s", entityLabel, recordId);
		
		// 审批人
		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, submitter);
		step.setID("recordId", recordId);
		step.setID("approvalId", approvalId);
		step.setString("node", recordOfMain.getString(EntityHelper.ApprovalStepNode));
		step.setString("prevNode", FlowNode.NODE_ROOT);
		for (ID a : nextApprovers) {
			Record clone = step.clone();
			clone.setID("approver", a);
			clone = super.create(clone);
			Application.getNotifications().send(MessageBuilder.createApproval(submitter, a, approvalMsg, clone.getPrimary()));
		}
		
		// 抄送人
		if (cc != null && !cc.isEmpty()) {
			String ccMsg = String.format("用户 @%s 提交了一条%s审批，请知晓 @%s", submitter, entityLabel, recordId);
			for (ID c : cc) {
				Application.getNotifications().send(MessageBuilder.createApproval(c, ccMsg));
			}
		}
		
		// see #getSubmitter
		String ckey = "ApprovalSubmitter" + recordId + approvalId;
		Application.getCommonCache().evict(ckey);
	}

	/**
	 * @param stepRecord
	 * @param signMode
	 * @param cc
	 * @param nextApprovers 驳回时无需
	 * @param nextNode 驳回时无需
	 * @param addedData 驳回时无需
	 */
	public void txApprove(Record stepRecord, String signMode, Set<ID> cc, Set<ID> nextApprovers, String nextNode, Record addedData) {
		// 审批时更新主记录
		if (addedData != null) {
			ADDED_MODE.set(true);
			try {
				Application.getService(addedData.getEntity().getEntityCode()).update(addedData);
			} finally {
				ADDED_MODE.remove();
			}
		}

		super.update(stepRecord);
		final ID stepRecordId = stepRecord.getPrimary();
		
		Object[] stepObject = Application.createQueryNoFilter(
				"select recordId,approvalId,node from RobotApprovalStep where stepId = ?")
				.setParameter(1, stepRecordId)
				.unique();
		final ID submitter = getSubmitter((ID) stepObject[0], (ID) stepObject[1]);
		final ID recordId = (ID) stepObject[0];
		final ID approvalId = (ID) stepObject[1];
		final String currentNode = (String) stepObject[2];
		final ID approver = Application.getCurrentUser();
		
		String entityLabel = EasyMeta.getLabel(MetadataHelper.getEntity(recordId.getEntityCode()));
		ApprovalState state = (ApprovalState) ApprovalState.valueOf(stepRecord.getInt("state"));
		
		// 抄送人
		if (cc != null && !cc.isEmpty()) {
			String ccMsg = String.format("用户 @%s 提交的%s审批由 @%s 已%s，请知晓 @%s",
					submitter, entityLabel, approver, state.getName(), recordId);
			for (ID c : cc) {
				Application.getNotifications().send(MessageBuilder.createApproval(c, ccMsg));
			}
		}
		
		// 拒绝了直接返回
		if (state == ApprovalState.REJECTED) {
			// 拒绝了，同一节点的其他审批人全部作废
			cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, true);

			// 更新主记录
			Record recordOfMain = EntityHelper.forUpdate(recordId, approver, false);
			recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.REJECTED.getState());
			super.update(recordOfMain);
			
			String rejectedMsg = String.format("@%s 驳回了你的%s审批 @%s", approver, entityLabel, recordId);
			Application.getNotifications().send(MessageBuilder.createApproval(submitter, rejectedMsg));
			return;
		}
		
		// 或签/会签
		boolean goNextNode = true;
		
		String approvalMsg = String.format("有一条%s记录请你审批 @%s", entityLabel, recordId);
		
		// 或签。一人通过其他作废
		if (FlowNode.SIGN_OR.equals(signMode)) {
			cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, false);
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
					Application.getNotifications().send(MessageBuilder.createApproval(submitter, (ID) o[1], approvalMsg, r.getPrimary()));
				}
			}
		}

		// 最终状态（审批通过）
		if (goNextNode && (nextApprovers == null || nextNode == null)) {
			approved(recordId, approver, null, null);
			return;
		}

		// 进入下一步
		if (goNextNode) {
			Record recordOfMain = EntityHelper.forUpdate(recordId, Application.getCurrentUser(), false);
			recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNode);
			super.update(recordOfMain);
		}
		
		// 审批人
		if (nextApprovers != null) {
			for (ID a : nextApprovers) {
				ID created = createStepIfNeed(recordId, approvalId, nextNode, a, !goNextNode, currentNode);

				// 非会签通知审批
				if (goNextNode && created != null) {
					Application.getNotifications().send(MessageBuilder.createApproval(submitter, a, approvalMsg, created));
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
	 * @param isRevoke 是否撤销，这是针对审批完成的
	 */
	public void txCancel(ID recordId, ID approvalId, String currentNode, boolean isRevoke) {
		final ID opUser = Application.getCurrentUser();
		final ApprovalState useState = isRevoke ? ApprovalState.REVOKED : ApprovalState.CANCELED;

		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, opUser);
		step.setID("recordId", recordId);
		step.setID("approvalId", approvalId == null ? APPROVAL_NOID : approvalId);
		step.setID("approver", opUser);
		step.setInt("state", useState.getState());
		step.setString("node", isRevoke ? FlowNode.NODE_REVOKED : FlowNode.NODE_CANCELED);
		step.setString("prevNode", currentNode);
		super.create(step);

		final Record recordOfMain = EntityHelper.forUpdate(recordId, opUser);
		recordOfMain.setInt(EntityHelper.ApprovalState, useState.getState());
		super.update(recordOfMain);

		// 撤销时触发器
		if (isRevoke) {
			Record before = recordOfMain.clone();
			before.setInt(EntityHelper.ApprovalState, ApprovalState.APPROVED.getState());
			new RobotTriggerManual().onRevoked(OperatingContext.create(opUser, BizzPermission.UPDATE, before, recordOfMain));
		}
	}

	/**
	 * @param recordId
	 * @param approvalId
	 * @param node
	 * @param approver
	 * @param isWaiting
	 * @return
	 */
	private ID createStepIfNeed(ID recordId, ID approvalId, String node, ID approver, boolean isWaiting, String prevNode) {
		Object[] hadApprover = Application.createQueryNoFilter(
				"select stepId from RobotApprovalStep where recordId = ? and approvalId = ? and node = ? and approver = ? and isCanceled = 'F'")
				.setParameter(1, recordId)
				.setParameter(2, approvalId)
				.setParameter(3, node)
				.setParameter(4, approver)
				.unique();
		if (hadApprover != null) {
			return null;
		}
		
		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, Application.getCurrentUser());
		step.setID("recordId", recordId);
		step.setID("approvalId", approvalId);
		step.setString("node", node);
		step.setID("approver", approver);
		if (isWaiting) {
			step.setBoolean("isWaiting", true);
		}
		if (prevNode != null) {
			step.setString("prevNode", prevNode);
		}
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
	 * @param onlyDarft
	 */
	private void cancelAliveSteps(ID recordId, ID approvalId, String node, ID excludeStep, boolean onlyDarft) {
		String sql = "select stepId from RobotApprovalStep where recordId = ? and isCanceled = 'F'";
		if (approvalId != null) {
            sql += " and approvalId = '" + approvalId + "'";
        }
		if (node != null) {
            sql += " and node = '" + node + "'";
        }
		if (onlyDarft) {
            sql += " and state = " + ApprovalState.DRAFT.getState();
        }

		Object[][] cancelled = Application.createQueryNoFilter(sql)
				.setParameter(1, recordId)
				.array();

		for (Object[] o : cancelled) {
			if (excludeStep != null && excludeStep.equals(o[0])) {
				continue;
			}
			Record step = EntityHelper.forUpdate((ID) o[0], Application.getCurrentUser());
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
		ID submitter = (ID) Application.getCommonCache().getx(ckey);
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
		Application.getCommonCache().putx(ckey, submitter);
		return submitter;
	}

	/**
	 * 审批通过
	 *
	 * @param recordId
	 * @param approver
	 * @param useApproval [自动审批时需要]
	 * @param useNode [自动审批时需要]
	 */
	private void approved(ID recordId, ID approver, ID useApproval, String useNode) {
		// 审批通过
		final Record recordOfMain = EntityHelper.forUpdate(recordId, approver, false);
		recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.APPROVED.getState());
		if (useApproval != null) recordOfMain.setID(EntityHelper.ApprovalId, useApproval);
		if (useNode != null) recordOfMain.setString(EntityHelper.ApprovalStepNode, useNode);
		super.update(recordOfMain);

		// 触发器
		Record before = recordOfMain.clone();
		before.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
		new RobotTriggerManual().onApproved(OperatingContext.create(approver, BizzPermission.UPDATE, before, recordOfMain));
	}

	/**
	 * 自动审批
	 *
	 * @param recordId
	 * @param useApprover
	 * @param useApproval
	 * @return
	 */
	public boolean txAutoApproved(ID recordId, ID useApprover, ID useApproval) {
		Object[] state = ApprovalHelper.getApprovalState(recordId);
		ApprovalState approvalState = (ApprovalState) ApprovalState.valueOf((Integer) state[2]);

		// 其他状态不能自动审批
		if (approvalState == ApprovalState.DRAFT || approvalState == ApprovalState.REJECTED
				|| approvalState == ApprovalState.REVOKED) {
			if (useApprover == null) useApprover = UserService.SYSTEM_USER;
			if (useApproval == null) useApproval = APPROVAL_NOID;

			ID stepId = createStepIfNeed(recordId, useApproval,
					FlowNode.NODE_AUTOAPPROVAL, useApprover, false, FlowNode.NODE_ROOT);
			Record step = EntityHelper.forUpdate(stepId, useApprover, false);
			step.setInt("state", ApprovalState.APPROVED.getState());
			step.setString("remark", "自动审批 (触发器)");
			super.update(step);

			approved(recordId, useApprover, useApproval, FlowNode.NODE_AUTOAPPROVAL);
			return true;
		}
		return false;
	}

	// --

	private static final ThreadLocal<Boolean> ADDED_MODE = new ThreadLocal<>();
	/**
	 * 可编辑字段模式
	 *
	 * @return
	 */
	public static boolean inAddedMode() {
		return ADDED_MODE.get() != null && ADDED_MODE.get();
	}
}
