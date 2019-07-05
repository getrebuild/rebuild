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

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;

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
			String sql = String.format("select approvalId from %s where %s = ?", entity.getName(), entity.getPrimaryField().getName());
			Object[] had =	Application.createQueryNoFilter(sql)
					.setParameter(1, record)
					.unique();
			if (had[0] != null) {
				return (ID) had[0];
			}
		}
		
		FlowDefinition[] defs = getFlowDefinitions(entity);
		for (FlowDefinition d : defs) {
			if (!d.isDisabled()) {
				return d.getID("id");
			}
		}
		return null;
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
			def.set("flowDefinition", o[0]);
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
