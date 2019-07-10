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
import java.util.HashSet;
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
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.Message;
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
	final private ID approval;
	
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
		final String nodeId = "ROOT";
		
		List<FlowNode> nextNodes = getNextNodes(nodeId);
		if (nextNodes.isEmpty()) {
			LOG.warn("No next-node be found");
			return false;
		}

		Set<ID> approvers = new HashSet<>();
		Set<ID> ccs = new HashSet<>();
		for (FlowNode node : nextNodes) {
			if (FlowNode.TYPE_APPROVER.equals(node.getType())) {
				approvers.addAll(node.getSpecUsers(user, record));
			} else {
				ccs.addAll(node.getSpecUsers(user, record));
			}
		}
		if (selectUsers != null) {
			approvers.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectApprovers"), record));
			ccs.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectCcs"), record));
		}
		
		if (approvers.isEmpty()) {
			LOG.warn("No any approvers special");
			return false;
		}
		
		Record record = EntityHelper.forUpdate(this.record, user);
		record.setID(EntityHelper.ApprovalId, this.approval);
		record.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
		record.setString(EntityHelper.ApprovalStepNode, nodeId);
		Application.getEntityService(this.record.getEntityCode()).update(record);
		
		// 审批人
		Record step = EntityHelper.forNew(EntityHelper.RobotApprovalStep, user);
		step.setID("recordId", this.record);
		step.setID("approvalId", this.approval);
		step.setString("node", nodeId);
		for (ID approver : approvers) {
			Record clone = step.clone();
			clone.setID("approver", approver);
			Application.getCommonService().create(clone);
		}
		
		// 抄送人
		for (ID cc : ccs) {
			Message message = new Message(cc, "审批", this.record);
			Application.getNotifications().send(message);
		}
		
		return true;
	}
	
	/**
	 * 审批
	 * 
	 * @param approver
	 * @param state
	 * @param remark
	 * @return
	 * @throws ApprovalException
	 */
	public boolean approve(ID approver, int state, String remark) throws ApprovalException {
		return false;
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
	public List<FlowNode> getNextNodes() {
		return getNextNodes(getCurrentNodeId());
	}
	
	/**
	 * 获取下一组节点。遇到审核人节点则终止，在审核节点前有抄送节点也会返回
	 * 
	 * @param currentNode
	 * @return
	 */
	public List<FlowNode> getNextNodes(String currentNode) {
		Assert.notNull(currentNode, "[currentNode] not be null");
		
		List<FlowNode> nodes = new ArrayList<FlowNode>();
		FlowNode next = null;
		while (true) {
			next = getNextNode(next != null ? next.getNodeId() : currentNode);
			if (next == null) {
				break;
			}
			
			nodes.add(next);
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
		Object[] stepNode = Application.getQueryFactory().unique(record, EntityHelper.ApprovalStepNode, EntityHelper.ApprovalState);
		String cNode = stepNode == null ? null : (String) stepNode[0];
		if (StringUtils.isBlank(cNode) || (Integer) stepNode[1] == ApprovalState.REJECTED.getState()) {
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
				MetadataHelper.getEntity(this.record.getEntityCode()), approval);
		flowParser = new FlowParser(flowDefinition.getJSON("flowDefinition"));
		return flowParser;
	}
	
	/**
	 * 获取已执行流程列表
	 * 
	 * @return returns [ [S,S], [S], [SSS], [S] ]
	 */
	public JSONArray getWorkedSteps() {
		Object[][] array = Application.createQuery(
				"select prevStepId.node,approver,state,remark,createdOn from RobotApprovalStep where recordId = ? and approvalId = ?")
				.setParameter(1, this.record)
				.setParameter(2, this.approval)
				.array();

		Map<String, List<Object[]>> stepsByPrev = new HashMap<>();
		for (Object[] o : array) {
			String prevNode = o[0] == null ? FlowNode.ROOT : (String) o[0];
			List<Object[]> steps = stepsByPrev.get(prevNode);
			if (steps == null) {
				steps = new ArrayList<Object[]>();
				stepsByPrev.put(prevNode, steps);
			}
			steps.add(o);
		}
		
		JSONArray steps = new JSONArray();
		String prev = FlowNode.ROOT;
		while (prev != null) {
			List<Object[]> group = stepsByPrev.get(prev);
			if (group == null) {
				break;
			}
			
			formatSteps(group, steps);
			prev = (String) group.get(0)[0];
		}
		return steps;
	}
	
	/**
	 * @param stepGroup
	 * @param dest
	 */
	private void formatSteps(List<Object[]> stepGroup, JSONArray dest) {
		JSONArray list = new JSONArray();
		for (Object[] o : stepGroup) {
			ID approver = (ID) o[1];
			JSONObject step = JSONUtils.toJSONObject(
					new String[] { "approver", "approverName", "state", "remark", "createdOn" }, 
					new Object[] { approver, UserHelper.getName(approver), o[2], o[3],
							CalendarUtils.getUTCDateTimeFormat().format(o[4]) });
			list.add(step);
		}
		dest.add(list);
	}
}
