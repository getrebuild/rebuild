/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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
