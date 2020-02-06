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

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.service.bizz.UserHelper;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * 1个审批节点+N个抄送节点
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/11
 * @see FlowNode
 */
public class FlowNodeGroup {
	
	private Set<FlowNode> nodes = new HashSet<>();
	
	protected FlowNodeGroup() {
		super();
	}
	
	/**
	 * @param node
	 */
	public void addNode(FlowNode node) {
		Assert.isNull(getApprovalNode(), "Cannot add multiple approved nodes");
		nodes.add(node);
	}
	
	/**
	 * @return
	 */
	public boolean allowSelfSelectingCc() {
		for (FlowNode node : nodes) {
			if (node.getType().equals(FlowNode.TYPE_CC) && node.allowSelfSelecting()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean allowSelfSelectingApprover() {
		FlowNode node = getApprovalNode();
		return node != null && node.allowSelfSelecting();
	}
	
	/**
	 * @param operator
	 * @param recordId
	 * @param selectUsers
	 * @return
	 */
	public Set<ID> getCcUsers(ID operator, ID recordId, JSONObject selectUsers) {
		Set<ID> users = new HashSet<>();
		for (FlowNode node : nodes) {
			if (FlowNode.TYPE_CC.equals(node.getType())) {
				users.addAll(node.getSpecUsers(operator, recordId));
			}
		}
		
		if (selectUsers != null) {
			users.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectCcs"), recordId));
		}
		return users;
	}
	
	/**
	 * @param operator
	 * @param recordId
	 * @param selectUsers
	 * @return
	 */
	public Set<ID> getApproveUsers(ID operator, ID recordId, JSONObject selectUsers) {
		Set<ID> users = new HashSet<>();

		FlowNode node = getApprovalNode();
		if (node != null) {
			users.addAll(node.getSpecUsers(operator, recordId));
		}

		if (selectUsers != null) {
			users.addAll(UserHelper.parseUsers(selectUsers.getJSONArray("selectApprovers"), recordId));
		}
		return users;
	}
	
	/**
	 * 如果没有审批节点了就当做最终审批
	 * 
	 * @return
	 */
	public boolean isLastStep() {
		// TODO 对审批最后一步加强判断
		return getApprovalNode() == null;
	}
	
	/**
	 * @return
	 */
	public boolean isValid() {
		return !nodes.isEmpty();
	}
	
	/**
	 * 获取审批节点
	 *
	 * @return
	 */
	public FlowNode getApprovalNode() {
		for (FlowNode node : nodes) {
			if (FlowNode.TYPE_APPROVER.equals(node.getType())) {
				return node;
			}
		}
		return null;
	}
	
	/**
	 * 联合审批模式
	 * 
	 * @return
	 */
	public String getSignMode() {
		FlowNode node = getApprovalNode();
		return node == null ? FlowNode.SIGN_OR : node.getSignMode();
	}
}
