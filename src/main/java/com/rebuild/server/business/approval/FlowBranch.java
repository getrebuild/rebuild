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

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.query.AdvFilterParser;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * 流程分支（条件）
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class FlowBranch extends FlowNode {

	private int priority;
	
	private Set<String> childNodes = new HashSet<>();
	private String lastNode;
	
	/**
	 * @param nodeId
	 * @param priority
	 * @param dataMap
	 */
	protected FlowBranch(String nodeId, int priority, JSONObject dataMap) {
		super(nodeId, TYPE_BRANCH, dataMap);
		this.priority = priority;
	}

	/**
	 * @return
	 */
	public int getPriority() {
		return priority;
	}
	
	/**
	 * @param child
	 */
	protected void addNode(String child) {
		childNodes.add(child);
		lastNode = child;
	}
	
	/**
	 * @return
	 */
	protected Set<String> getChildNodes() {
		return childNodes;
	}
	
	/**
	 * @return
	 */
	protected String getLastNode() {
		return lastNode;
	}
	
	/**
	 * 匹配条件分支
	 * 
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
			
			String sql = MessageFormat.format("select {0} from {1} where {2} and {0} = ?", 
					entity.getPrimaryField().getName(), entity.getName(), sqlWhere);
			Object[] matches = Application.createQueryNoFilter(sql).setParameter(1, record).unique();
			return matches != null;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return super.toString() + ", Priority:" + getPriority();
	}
	
	/**
	 * @param node
	 * @return
	 */
	public static FlowBranch valueOf(JSONObject node) {
		return new FlowBranch(
				node.getString("nodeId"), node.getIntValue("priority"), node.getJSONObject("data"));
	}
}
