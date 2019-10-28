/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.ApprovalState;
import com.rebuild.server.business.approval.FlowNode;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.notification.MessageBuilder;

import java.util.Set;

/**
 * 审批流程。
 *
 * isWaiting - 因为会签的关系还不能进入下一步审批，因此需要等待。待会签完毕，此值将更新为 true
 * isCanceled - 是否作废。例如或签中，一人同意其他即作废
 *
 * @author devezhao
 * @since 07/11/2019
 */
public class ApprovalStepService extends BaseService {

	protected ApprovalStepService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}

	@Override
	public int getEntityCode() {
		return EntityHelper.RobotApprovalStep;
	}
	
	/**
	 * @param mainRecord
	 * @param cc
	 * @param nextApprovers
	 */
	public void txSubmit(Record mainRecord, Set<ID> cc, Set<ID> nextApprovers) {
		final ID submitter = Application.getCurrentUser();
		final ID recordId = mainRecord.getPrimary();
		final ID approvalId = mainRecord.getID(EntityHelper.ApprovalId);
		
		// 作废之前的步骤（若有）
		cancelAliveSteps(recordId, null, null, null, false);

		super.update(mainRecord);
		
		String entityLabel = EasyMeta.getLabel(mainRecord.getEntity());
		String approveMsg = String.format("有一条%s记录请你审批 @%s", entityLabel, recordId);
		
		// 审批人
		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, submitter);
		step.setID("recordId", recordId);
		step.setID("approvalId", approvalId);
		step.setString("node", mainRecord.getString(EntityHelper.ApprovalStepNode));
		step.setString("prevNode", FlowNode.NODE_ROOT);
		for (ID a : nextApprovers) {
			Record clone = step.clone();
			clone.setID("approver", a);
			clone = super.create(clone);
			Application.getNotifications().send(MessageBuilder.createApproval(submitter, a, approveMsg, clone.getPrimary()));
		}
		
		// 抄送人
		if (cc != null && !cc.isEmpty()) {
			String ccMsg = String.format("用户 @%s 提交了一条%s审批，请知晓 @%s", submitter, entityLabel, recordId);
			for (ID c : cc) {
				Application.getNotifications().send(MessageBuilder.createApproval(c, ccMsg));
			}
		}
		
		// see #findSubmitter
		String cKey = "ApprovalSubmitter" + recordId + approvalId;
		Application.getCommonCache().evict(cKey);
	}
	
	/**
	 * @param stepRecord
	 * @param signMode
	 * @param cc
	 * @param nextApprovers
	 * @param nextNode
	 */
	public void txApprove(Record stepRecord, String signMode, Set<ID> cc, Set<ID> nextApprovers, String nextNode) {
		super.update(stepRecord);
		final ID stepRecordId = stepRecord.getPrimary();
		
		Object[] stepObject = Application.createQueryNoFilter(
				"select recordId,approvalId,node from RobotApprovalStep where stepId = ?")
				.setParameter(1, stepRecordId)
				.unique();
		final ID submitter = findSubmitter((ID) stepObject[0], (ID) stepObject[1]);
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
			// 更新联合审批
			cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, true);

			// 更新主记录
			Record main = EntityHelper.forUpdate(recordId, Application.getCurrentUser(), false);
			main.setInt(EntityHelper.ApprovalState, ApprovalState.REJECTED.getState());
			super.update(main);
			
			String rejectMsg = String.format("@%s 驳回了你的%s审批 @%s", approver, entityLabel, recordId);
			Application.getNotifications().send(MessageBuilder.createApproval(submitter, rejectMsg));
			return;
		}
		
		// 或签/会签
		boolean goNextNode = true;
		
		String approveMsg = String.format("有一条%s记录请你审批 @%s", entityLabel, recordId);
		
		// 或签。一人通过其他作废
		if (FlowNode.SIGN_OR.equals(signMode)) {
//			cancelAliveSteps(recordId, approvalId, currentNode, stepRecordId, false);
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
					Application.getNotifications().send(MessageBuilder.createApproval(submitter, (ID) o[1], approveMsg, r.getPrimary()));
				}
			}
		}

		// 最终状态了
		if (goNextNode && (nextApprovers == null || nextNode == null)) {
			Record main = EntityHelper.forUpdate(recordId, Application.getCurrentUser(), false);
			main.setInt(EntityHelper.ApprovalState, ApprovalState.APPROVED.getState());
			super.update(main);
			return;
		}
		
		// 进入下一步
		if (goNextNode) {
			Record main = EntityHelper.forUpdate(recordId, Application.getCurrentUser(), false);
			main.setString(EntityHelper.ApprovalStepNode, nextNode);
			super.update(main);
		}
		
		// 审批人
		if (nextApprovers != null) {
			for (ID a : nextApprovers) {
				ID created = createStepIfNeed(recordId, approvalId, nextNode, a, !goNextNode, currentNode);

				// 非会签通知审批
				if (goNextNode && created != null) {
					Application.getNotifications().send(MessageBuilder.createApproval(submitter, a, approveMsg, created));
				}
			}
		}
	}

	/**
	 * @param recordId
	 * @param approvalId
	 * @param currentNode
	 */
	public void txCancel(ID recordId, ID approvalId, String currentNode) {
		final ID canceller = Application.getCurrentUser();

		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, canceller);
		step.setID("recordId", recordId);
		step.setID("approvalId", approvalId);
		step.setID("approver", canceller);
		step.setInt("state", ApprovalState.CANCELED.getState());
		step.setString("node", FlowNode.NODE_CANCELED);
		step.setString("prevNode", currentNode);
		super.create(step);

		Record main = EntityHelper.forUpdate(recordId, canceller);
		main.setInt(EntityHelper.ApprovalState, ApprovalState.CANCELED.getState());
		super.update(main);
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
		if (approvalId != null) sql += " and approvalId = '" + approvalId + "'";
		if (node != null) sql += " and node = '" + node + "'";
		if (onlyDarft) sql += " and state = " + ApprovalState.DRAFT.getState();

		Object[][] cancelled = Application.createQueryNoFilter(sql)
				.setParameter(1, recordId)
				.array();

		for (Object[] o : cancelled) {
			if (excludeStep != null && excludeStep.equals(o[0])) {
				continue;
			}
			Record r = EntityHelper.forUpdate((ID) o[0], Application.getCurrentUser());
			r.setBoolean("isCanceled", true);
			super.update(r);
		}
	}
	
	/**
	 * 审批提交人
	 *
	 * @param recordId
	 * @param approvalId
	 * @return
	 */
	public ID findSubmitter(ID recordId, ID approvalId) {
		String cKey = "ApprovalSubmitter" + recordId + approvalId;
		ID submitter = (ID) Application.getCommonCache().getx(cKey);
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
		Application.getCommonCache().putx(cKey, submitter);
		return submitter;
	}
}
