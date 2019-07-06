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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.query.AdvFilterParser;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 流程分支（条件）
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class FlowBranch extends FlowNode {

	private Set<FlowNode> childNodes = new HashSet<>();
	
	/**
	 * @param nodeId
	 * @param dataMap
	 */
	protected FlowBranch(String nodeId, JSONObject dataMap) {
		super(nodeId, TYPE_BRANCH, dataMap);
	}
	
	/**
	 * @param child
	 */
	protected void addNode(FlowNode child) {
		childNodes.add(child);
	}
	
	/**
	 * @return
	 */
	protected Set<FlowNode> getChildNodes() {
		return childNodes;
	}
	
	/**
	 * @param record
	 * @return
	 */
	public boolean matches(ID record) {
		Entity entity = MetadataHelper.getEntity(record.getEntityCode());
		JSONObject filterExp = (JSONObject) getDataMap().get("filter");
		JSONArray filterItems = filterExp == null ? null : filterExp.getJSONArray("items");
		if (filterItems != null && !filterItems.isEmpty()) {
			AdvFilterParser filterParser = new AdvFilterParser(filterExp);
			String sqlWhere = filterParser.toSqlWhere();
			String sql = String.format("select %s from %s where (1=1) and %s = ?", 
					entity.getPrimaryField().getName(), entity.getName(), sqlWhere);
			Object[] matches = Application.createQuery(sql).setParameter(1, record).unique();
			return matches != null;
		}
		return true;
	}
	
	/**
	 * @param node
	 * @return
	 */
	public static FlowBranch valueOf(JSONObject node) {
		return new FlowBranch(
				node.getString("nodeId"), node.getJSONObject("data"));
	}
}
