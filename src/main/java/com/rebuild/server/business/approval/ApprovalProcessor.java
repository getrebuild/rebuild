/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.approval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.FlowDefinition;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.ApprovalStepService;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 审批处理
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
public class ApprovalProcessor {

	private  static final Log LOG = LogFactory.getLog(ApprovalProcessor.class);

	final private ID user;
	final private ID record;
	
	private ID approval;
	private FlowParser flowParser;
	
	/**
	 * @param user
	 * @param record
	 */
	public ApprovalProcessor(ID user, ID record) {
		this(user, record, null);
	}
	
	/**
	 * @param user
	 * @param record
	 * @param approval
	 */
	public ApprovalProcessor(ID user, ID record, ID approval) {
		this.user = user;
		this.record = record;
		this.approval = approval;
	}
	
	/**
	 * 提交
	 * 
	 * @param selectUsers
	 * @return
	 * @throws ApprovalException
	 */
	public boolean submit(JSONObject selectUsers) throws ApprovalException {
		FlowNodeGroup nextNodes = getNextNodes(FlowNode.ROOT);
		if (!nextNodes.isValid()) {
			LOG.warn("No next-node be found");
			return false;
		}

		Set<ID> ccs = nextNodes.getCcUsers(this.user, this.record, selectUsers);
		Set<ID> nextApprovers = nextNodes.getApproveUsers(this.user, this.record, selectUsers);
		if (nextApprovers.isEmpty()) {
			LOG.warn("No any approvers special");
			return false;
		}
		
		Record recordOfMain = EntityHelper.forUpdate(this.record, this.user);
		recordOfMain.setID(EntityHelper.ApprovalId, this.approval);
		recordOfMain.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
		recordOfMain.setString(EntityHelper.ApprovalStepNode, nextNodes.getStepNode());
		Application.getBean(ApprovalStepService.class).txSubmit(recordOfMain, ccs, nextApprovers);
		return true;
	}
	
	/**
	 * 审批
	 * 
	 * @param approver
	 * @param state
	 * @param remark
	 * @param selectUsers
	 * @return
	 * @throws ApprovalException
	 */
	public boolean approve(ID approver, int state, String remark, JSONObject selectUsers) throws ApprovalException {
		final Object step[] = Application.createQueryNoFilter(
				"select stepId,state,node,approvalId from RobotApprovalStep where recordId = ? and approver = ?")
				.setParameter(1, this.record)
				.setParameter(2, approver)
				.unique();
		if (step == null || (Integer) step[1] != ApprovalState.DRAFT.getState()) {
			LOG.warn("Invalid step state");
			return false;
		}
		
		Record approvedStep = EntityHelper.forUpdate((ID) step[0], approver);
		approvedStep.setInt("state", state);
		approvedStep.setDate("approvedTime", CalendarUtils.now());
		if (StringUtils.isNotBlank(remark)) {
			approvedStep.setString("remark", remark);
		}
		
		this.approval = (ID) step[3];
		FlowNodeGroup nextNodes = getNextNodes((String) step[2]);
		
		Set<ID> ccs = nextNodes.getCcUsers(this.user, this.record, selectUsers);
		Set<ID> nextApprovers = null;
		String nextNode = null;
		if (!nextNodes.isLastStep()) {
			nextApprovers = nextNodes.getApproveUsers(this.user, this.record, selectUsers);
			if (nextApprovers.isEmpty()) {
				LOG.warn("No any approvers special");
				return false;
			}
			nextNode = nextNodes.getStepNode();
		}
		
		Application.getBean(ApprovalStepService.class)
				.txApprove(approvedStep, nextNodes.getSignMode(), ccs, nextApprovers, nextNode);
		return true;
	}
	
	/**
	 * 获取下一审批节点
	 * 
	 * @return
	 */
	public FlowNode getNextApprovalNode() {
		FlowNode next = null;
		while (true) {
			next = getNextNode();
			if (next == null || FlowNode.TYPE_APPROVER.equals(next.getType())) {
				break;
			}
		}
		return next;
	}
	
	/**
	 * @return
	 * @see #getNextNode(String)
	 */
	public FlowNode getNextNode() {
		return getNextNode(getCurrentNodeId());
	}
	
	/**
	 * 获取下一节点
	 * 
	 * @param currentNode
	 * @return
	 */
	public FlowNode getNextNode(String currentNode) {
		Assert.notNull(currentNode, "[currentNode] not be null");
		
		List<FlowNode> nextNodes = getFlowParser().getNextNodes(currentNode);
		if (nextNodes.isEmpty()) {
			return null;
		}
		
		FlowNode firstNode = nextNodes.get(0);
		if (!FlowNode.TYPE_BRANCH.equals(firstNode.getType())) {
			return firstNode;
		}
		
		for (FlowNode node : nextNodes) {
			FlowBranch branch = (FlowBranch) node;
			if (branch.matches(record)) {
				return getNextNode(branch.getNodeId());
			}
		}
		return null;
	}
	
	/**
	 * @return
	 * @see #getNextNodes(String)
	 */
	public FlowNodeGroup getNextNodes() {
		return getNextNodes(getCurrentNodeId());
	}
	
	/**
	 * 获取下一组节点。遇到审核人节点则终止，在审核节点前有抄送节点也会返回
	 * 
	 * @param currentNode
	 * @return
	 */
	public FlowNodeGroup getNextNodes(String currentNode) {
		Assert.notNull(currentNode, "[currentNode] not be null");
		
		FlowNodeGroup nodes = new FlowNodeGroup();
		FlowNode next = null;
		while (true) {
			next = getNextNode(next != null ? next.getNodeId() : currentNode);
			if (next == null) {
				break;
			}
			
			nodes.addNode(next);
			if (FlowNode.TYPE_APPROVER.equals(next.getType())) {
				break;
			}
		}
		return nodes;
	}
	
	/**
	 * @return
	 */
	private String getCurrentNodeId() {
		Object[] stepNode = Application.getQueryFactory().unique(this.record, EntityHelper.ApprovalStepNode, EntityHelper.ApprovalState);
		String cNode = stepNode == null ? null : (String) stepNode[0];
		if (StringUtils.isBlank(cNode) || (Integer) stepNode[1] >= ApprovalState.REJECTED.getState()) {
			cNode = "ROOT";
		}
		return cNode;
	}
	
	/**
	 * @return
	 */
	private FlowParser getFlowParser() {
		if (flowParser != null) {
			return flowParser;
		}
		
		FlowDefinition flowDefinition = RobotApprovalManager.instance.getFlowDefinition(
				MetadataHelper.getEntity(this.record.getEntityCode()), this.approval);
		flowParser = new FlowParser(flowDefinition.getJSON("flowDefinition"));
		return flowParser;
	}
	
	/**
	 * 获取当前审批步骤
	 * 
	 * @return returns [S, S]
	 */
	public JSONArray getCurrentStep() {
		Object[] currentNode = Application.getQueryFactory().unique(this.record, EntityHelper.ApprovalStepNode);
		Object[][] array = Application.createQueryNoFilter(
				"select approver,state,remark,approvedTime,createdOn from RobotApprovalStep where recordId = ? and approvalId = ? and node = ?")
				.setParameter(1, this.record)
				.setParameter(2, this.approval)
				.setParameter(3, currentNode[0])
				.array();
		
		JSONArray state = new JSONArray();
		for (Object[] o : array) {
			state.add(this.formatStep(o));
		}
		return state;
	}
	
	/**
	 * 获取已执行流程列表
	 * 
	 * @return returns [ [S,S], [S], [SSS], [S] ]
	 */
	public JSONArray getWorkedSteps() {
		Object[][] array = Application.createQueryNoFilter(
				"select approver,state,remark,approvedTime,createdOn,prevStepId.node from RobotApprovalStep where recordId = ? and approvalId = ?")
				.setParameter(1, this.record)
				.setParameter(2, this.approval)
				.array();

		Map<String, List<Object[]>> stepGroupByPrev = new HashMap<>();
		for (Object[] o : array) {
			String prevNode = o[0] == null ? FlowNode.ROOT : (String) o[0];
			List<Object[]> steps = stepGroupByPrev.get(prevNode);
			if (steps == null) {
				steps = new ArrayList<Object[]>();
				stepGroupByPrev.put(prevNode, steps);
			}
			steps.add(o);
		}
		
		JSONArray steps = new JSONArray();
		String prev = FlowNode.ROOT;
		while (prev != null) {
			List<Object[]> group = stepGroupByPrev.get(prev);
			if (group == null) {
				break;
			}
			
			JSONArray state = new JSONArray();
			for (Object[] o : group) {
				state.add(formatStep(o));
			}
			steps.add(state);
			prev = (String) group.get(0)[5];
		}
		return steps;
	}
	
	/**
	 * @param step
	 */
	private JSONObject formatStep(Object[] step) {
		ID approver = (ID) step[0];
		return JSONUtils.toJSONObject(
				new String[] { "approver", "approverName", "state", "remark", "approvedTime", "createdOn" }, 
				new Object[] {
						approver, UserHelper.getName(approver), 
						step[1], step[2],
						step[3] != null ? CalendarUtils.getUTCDateTimeFormat().format(step[3]) : null,
						CalendarUtils.getUTCDateTimeFormat().format(step[4]) });
	}
}
