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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.FlowDefinition;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
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

	final private ID user;
	final private ID record;
	final private ID approval;
	
	private FlowParser flowParser;
	
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
	 * @return
	 * @throws ApprovalException
	 */
	public boolean submit() throws ApprovalException {
		Set<FlowNode> nextNodes = getNextNodes("ROOT");
		if (nextNodes.size() != 1) {
			return false;
		}
		
		Record record = EntityHelper.forUpdate(this.record, user);
		record.setID(EntityHelper.ApprovalId, this.approval);
		record.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
		record.setString(EntityHelper.ApprovalStepNode, "ROOT");
		Application.getService(this.record.getEntityCode()).update(record);
		
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
	public boolean submit(ID approver, int state, String remark) throws ApprovalException {
		return false;
	}
	
	/**
	 * @return
	 */
	public FlowNode getCurrentNode() {
		Object[] stepNode = Application.getQueryFactory().unique(record, EntityHelper.ApprovalStepNode);
		return getFlowParser().getNode((String) stepNode[0]);
	}
	
	/**
	 * @return
	 */
	public Set<FlowNode> getNextNodes() {
		FlowNode current = getCurrentNode();
		return getNextNodes(current.getNodeId());
	}
	
	/**
	 * @param currentNode
	 * @return
	 */
	private Set<FlowNode> getNextNodes(String currentNode) {
		Set<FlowNode> nextNodes = getFlowParser().getNextNodes(currentNode);
		if (nextNodes.isEmpty()) {
			return Collections.emptySet();
		}
		
		String nodeType = nextNodes.iterator().next().getType();
		if (!FlowNode.TYPE_BRANCH.equals(nodeType)) {
			return nextNodes;
		}
		
		Set<FlowNode> nextNodesByBranch = null;
		for (Iterator<FlowNode> iter = nextNodes.iterator(); iter.hasNext(); ) {
			FlowBranch branch = (FlowBranch) iter.next();
			if (branch.matches(record)) {
				nextNodesByBranch = branch.getChildNodes();
				break;
			}
		}
		return nextNodesByBranch == null ? nextNodes : nextNodesByBranch;
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
		Assert.notNull(approval, "[approval] not be null");
		
		Object[][] array = Application.createQuery(
				"select prevStepId,approver,state,createdOn,stepId from RobotApprovalStep where recordId = ? and approvalId = ?")
				.setParameter(1, this.record)
				.setParameter(2, this.approval)
				.array();

		Map<ID, List<Object[]>> stepsByPrev = new HashMap<>();
		for (Object[] o : array) {
			ID prev = o[0] == null ? this.record : (ID) o[0];
			List<Object[]> steps = stepsByPrev.get(prev);
			if (steps == null) {
				steps = new ArrayList<Object[]>();
				stepsByPrev.put(prev, steps);
			}
			steps.add(o);
		}
		
		JSONArray steps = new JSONArray();
		
		List<Object[]> root = stepsByPrev.remove(this.approval);
		Set<ID> prevIds = formatSteps(root, steps);
		
		while (true) {
			Set<ID> lastPrevIds = new HashSet<ID>();
			for (ID prev : prevIds) {
				List<Object[]> next = stepsByPrev.get(prev);
				if (next != null) {
					lastPrevIds.addAll(formatSteps(next, steps));
					stepsByPrev.remove(prev);
				}
			}
			
			prevIds = lastPrevIds;
			if (prevIds.isEmpty()) {
				break;
			}
		}
		
		return steps;
	}
	
	/**
	 * @param steps
	 * @param dest
	 * @return
	 */
	private Set<ID> formatSteps(List<Object[]> steps, JSONArray dest) {
		Set<ID> ids = new HashSet<>();
		JSONArray list = new JSONArray();
		for (Object[] o : steps) {
			ID approver = (ID) o[1];
			JSONObject step = JSONUtils.toJSONObject(
					new String[] { "approver", "approverName", "state", "createdOn" }, 
					new Object[] { approver, UserHelper.getName(approver), o[2], 
							CalendarUtils.getUTCDateTimeFormat().format(o[3]) });
			list.add(step);
			ids.add((ID) o[4]);
		}
		dest.add(list);
		return ids;
	}
	
}
