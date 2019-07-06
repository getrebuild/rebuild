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
	 * @param user
	 * @return
	 */
	public boolean matchesUser(ID user) {
		JSONArray users = getDataMap().getJSONArray("users");
		if (users.isEmpty()) {
			return true;
		}
		
		String userType = users.get(0).toString();
		if ("ALL".equalsIgnoreCase(userType)) {
			return true;
		}
		// TODO 提交人自己
		if ("SELF".equals(userType)) {
		}
		
		List<String> usersList = new ArrayList<>();
		for (Object o : users) {
			usersList.add((String) o);
		}
		Set<ID> usersAll = UserHelper.parseUsers(usersList, null);
		return usersAll.contains(user);
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
