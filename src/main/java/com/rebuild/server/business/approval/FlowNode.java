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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.engine.ID;

/**
 * 流程节点
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class FlowNode {
	
	// 节点类型
	
	public static final String TYPE_START = "start";
	public static final String TYPE_APPROVER = "approver";
	public static final String TYPE_CC = "cc";
	public static final String TYPE_CONDITION = "condition";
	public static final String TYPE_BRANCH = "branch";
	
	// 人员类型
	
	public static final String USER_ALL = "ALL";
	public static final String USER_SELF = "SELF";
	public static final String USER_SPEC = "SPEC";
	
	// --
	
	private String nodeId;
	private String type;
	private JSONObject dataMap;
	
	protected String prevNodes;
	
	/**
	 * @param nodeId
	 * @param type
	 * @param dataMap
	 */
	protected FlowNode(String nodeId, String type, JSONObject dataMap) {
		super();
		this.nodeId = nodeId;
		this.type = type;
		this.dataMap = dataMap;
	}

	/**
	 * @return
	 */
	public String getNodeId() {
		return nodeId;
	}
	
	/**
	 * @return
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @return
	 */
	public JSONObject getDataMap() {
		return dataMap == null ? JSONUtils.EMPTY_OBJECT : dataMap;
	}
	
	/**
	 * @param operator
	 * @param submitter
	 * @return
	 */
	public boolean matchesUser(ID operator, ID submitter) {
		JSONArray users = getDataMap().getJSONArray("users");
		if (users == null || users.isEmpty()) {
			return true;
		}
		
		String userType = users.get(0).toString();
		if (USER_ALL.equalsIgnoreCase(userType)) {
			return true;
		}
		if (USER_SELF.equals(userType) && operator.equals(submitter)) {
			return true;
		}
		
		List<String> usersList = new ArrayList<>();
		for (Object o : users) {
			usersList.add((String) o);
		}
		Set<ID> usersAll = UserHelper.parseUsers(usersList, null);
		return usersAll.contains(submitter);
	}
	
	/**
	 * 获取相关人员（提交人/审批人/抄送人）
	 * 
	 * @param operator
	 * @return
	 */
	public Set<ID> getSpecUsers(ID operator) {
		JSONArray userDefs = getDataMap().getJSONArray("users");
		if (userDefs == null || userDefs.isEmpty()) {
			return Collections.emptySet();
		}
		
		String userType = userDefs.get(0).toString();
		if (USER_SELF.equalsIgnoreCase(userType)) {
			Set<ID> users = new HashSet<ID>();
			users.add(operator);
			return users;
		}
		
		if (USER_SPEC.equalsIgnoreCase(userType)) {
			List<String> defsList = new ArrayList<>();
			for (Object o : userDefs) {
				defsList.add((String) o);
			}
			return UserHelper.parseUsers(defsList, null);
		}
		return Collections.emptySet();
	}
	
	@Override
	public String toString() {
		String string = String.format("Id:%s, Type:%s", nodeId, type);
		if (prevNodes != null) {
			string += ", Prev:" + prevNodes;
		}
		if (dataMap != null) {
			string += ", Data:" + dataMap.toJSONString();
		}
		return string;
	}

	/**
	 * @param node
	 * @return
	 */
	public static FlowNode valueOf(JSONObject node) {
		return new FlowNode(
				node.getString("nodeId"), node.getString("type"), node.getJSONObject("data"));
	}
}
