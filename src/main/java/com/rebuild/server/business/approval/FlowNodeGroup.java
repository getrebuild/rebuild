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

import java.util.HashSet;
import java.util.Set;

import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/07/11
 */
public class FlowNodeGroup {
	
	private Set<FlowNode> nodes = new HashSet<>();
	
	protected FlowNodeGroup() {
	}
	
	/**
	 * @param node
	 */
	public void addNode(FlowNode node) {
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
		for (FlowNode node : nodes) {
			if (node.getType().equals(FlowNode.TYPE_APPROVER) && node.allowSelfSelecting()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param user
	 * @param recordId
	 * @return
	 */
	public Set<ID> getSpecUsersCc(ID user, ID recordId) {
		Set<ID> users = new HashSet<>();
		for (FlowNode node : nodes) {
			if (FlowNode.TYPE_CC.equals(node.getType())) {
				users.addAll(node.getSpecUsers(user, recordId));
			}
		}
		return users;
	}
	
	/**
	 * @param user
	 * @param recordId
	 * @return
	 */
	public Set<ID> getSpecUsersApprove(ID user, ID recordId) {
		Set<ID> users = new HashSet<>();
		for (FlowNode node : nodes) {
			if (FlowNode.TYPE_APPROVER.equals(node.getType())) {
				users.addAll(node.getSpecUsers(user, recordId));
			}
		}
		return users;
	}
	
	/**
	 * 如果没有审批节点了就当做最终审批
	 * 
	 * @return
	 */
	public boolean isLastStep() {
		for (FlowNode node : nodes) {
			if (node.getType().equals(FlowNode.TYPE_APPROVER)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @return
	 */
	public boolean isValid() {
		return !nodes.isEmpty();
	}
	
	/**
	 * @return
	 */
	public String getStepNode() {
		for (FlowNode node : nodes) {
			if (FlowNode.TYPE_APPROVER.equals(node.getType())) {
				return node.getNodeId();
			}
		}
		return null;
	}
}
