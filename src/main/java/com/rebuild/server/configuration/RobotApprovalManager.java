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

package com.rebuild.server.configuration;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.business.approval.FlowNode;
import com.rebuild.server.business.approval.FlowParser;
import com.rebuild.server.helper.ConfigurationException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 审批流程管理
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
public class RobotApprovalManager implements ConfigManager<Entity> {

	public static final RobotApprovalManager instance = new RobotApprovalManager();
	private RobotApprovalManager() {}
	
	/**
	 * @param entity
	 * @return
	 */
	public ID findApproval(Entity entity) {
		return findApproval(entity, null);
	}
	
	/**
	 * 获取指定实体的可用流程
	 * 
	 * @param entity
	 * @param record
	 * @return
	 */
	public ID findApproval(Entity entity, ID record) {
		if (!entity.containsField(EntityHelper.ApprovalId)) {
			return null;
		}
		
		if (record != null) {
			Object[] hadApproval =	Application.getQueryFactory().unique(record, EntityHelper.ApprovalId);
			if (hadApproval[0] != null) {
				return (ID) hadApproval[0];
			}
		}
		
		FlowDefinition[] defs = getFlowDefinitions(entity);
		for (FlowDefinition def : defs) {
			if (!def.isDisabled()) {
				return def.getID("id");
			}
		}
		return null;
	}
	
	/**
	 * @param entity
	 * @param approvalId
	 * @return
	 */
	public FlowDefinition getFlowDefinition(Entity entity, ID approvalId) {
		FlowDefinition[] defs = getFlowDefinitions(entity);
		for (FlowDefinition def : defs) {
			if (approvalId.equals(def.getID("id"))) {
				return def;
			}
		}
		throw new ConfigurationException("No approval found : " + approvalId);
	}
	
	/**
	 * 获取用户可用流程
	 * 
	 * @param record
	 * @param user
	 * @return
	 */
	public FlowDefinition[] getFlowDefinitions(ID record, ID user) {
		FlowDefinition[] defs = getFlowDefinitions(MetadataHelper.getEntity(record.getEntityCode()));
		if (defs.length == 0) {
			return new FlowDefinition[0];
		}
		
		// 过滤可用的
		List<FlowDefinition> workable = new ArrayList<>();
		for (FlowDefinition def : defs) {
			if (def.isDisabled()) {
				continue;
			}
			
			FlowParser flowParser = def.createFlowParser();
			FlowNode root = flowParser.getNode("ROOT");
			
			// 发起人匹配
			JSONArray users = root.getDataMap().getJSONArray("users");
			if (users == null || users.isEmpty() 
					|| FlowNode.USER_ALL.equalsIgnoreCase(users.getString(0))
					|| UserHelper.parseUsers(users, record).contains(user)) {
				workable.add(def);
			}
		}
		return workable.toArray(new FlowDefinition[workable.size()]);
	}
	/**
	 * 获取指定实体的所有审核流程（含禁用的）
	 * 
	 * @param entity
	 * @return
	 */
	public FlowDefinition[] getFlowDefinitions(Entity entity) {
		final String cKey = "RobotApprovalManager-" + entity.getName();
		FlowDefinition[] defs = (FlowDefinition[]) Application.getCommonCache().getx(cKey);
		if (defs != null) {
			return defs;
		}
		
		Object[][] array = Application.createQueryNoFilter(
				"select flowDefinition,isDisabled,name,configId from RobotApprovalConfig where belongEntity = ?")
				.setParameter(1, entity.getName())
				.array();
		
		List<FlowDefinition> list = new ArrayList<>();
		for (Object[] o : array) {
			FlowDefinition def = new FlowDefinition();
			def.set("flowDefinition", JSON.parseObject((String) o[0]));
			def.set("disabled", o[1]);
			def.set("name", o[2]);
			def.set("id", o[3]);
			list.add(def);
		}
		
		defs = list.toArray(new FlowDefinition[list.size()]);
		Application.getCommonCache().putx(cKey, defs);
		return defs;
	}
	
	@Override
	public void clean(Entity cacheKey) {
		final String cKey = "RobotApprovalManager-" + cacheKey.getName();
		Application.getCommonCache().evict(cKey);
	}
}
