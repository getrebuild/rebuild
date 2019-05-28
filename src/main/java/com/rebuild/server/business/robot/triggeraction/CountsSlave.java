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

package com.rebuild.server.business.robot.triggeraction;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.robot.ActionContext;
import com.rebuild.server.business.robot.ActionType;
import com.rebuild.server.business.robot.TriggerAction;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/23
 */
public class CountsSlave implements TriggerAction {
	
	final private ActionContext context;
	
	public CountsSlave(ActionContext context) {
		this.context = context;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.COUNTSSLAVE;
	}
	
	@Override
	public boolean isUsableSourceEntity(int entityCode) {
		return MetadataHelper.isSlaveEntity(entityCode);
	}
	
	@Override
	public void execute() {
		final Entity slaveEntity = context.getSourceEntity();
		final Entity masterEntity = slaveEntity.getMasterEntity();
		final JSONObject actionContent = (JSONObject) context.getActionContent();
		
		String sourceField = actionContent.getString("sourceField");
		String targetField = actionContent.getString("targetField");
		if (!slaveEntity.containsField(sourceField)) {
			LOG.warn("Unknow field '" + sourceField + "' in '" + slaveEntity.getName() + "'");
			return;
		}
		if (!masterEntity.containsField(targetField)) {
			LOG.warn("Unknow field '" + targetField + "' in '" + masterEntity.getName() + "'");
			return;
		}
		
		// 找到主纪录
		Field stmField = MetadataHelper.getSlaveToMasterField(slaveEntity);
		String sql = String.format("select %s from %s where %s = ?", 
				stmField.getName(), slaveEntity.getName(), slaveEntity.getPrimaryField().getName());
		Object masterRecord[] = Application.createQueryNoFilter(sql).setParameter(1, context.getSourceRecord()).unique();
		if (masterRecord == null || masterRecord[0] == null) {
			LOG.warn("No record found by slave: " + context.getSourceRecord());
			return;
		}
		
		// 直接利用SQL计算结果
		String calcMode = actionContent.getString("calcMode");
		String calcField = "count".equalsIgnoreCase(calcMode) ? slaveEntity.getPrimaryField().getName() : sourceField;
		
		sql = String.format("select %s(%s) from %s where %s = ?", 
				calcMode.toLowerCase(), calcField, slaveEntity.getName(), stmField.getName());
		Object[] result = Application.createQueryNoFilter(sql).setParameter(1, masterRecord[0]).unique();
		Double calcValue = result == null || result[0] == null ? 0d : ObjectUtils.toDouble(result[0]);
		
		DisplayType dt = EasyMeta.getDisplayType(masterEntity.getField(targetField));
		Record master = EntityHelper.forUpdate((ID) masterRecord[0], UserService.SYSTEM_USER);
		if (dt == DisplayType.NUMBER) {
			master.setInt(targetField, calcValue.intValue());
		} else if (dt == DisplayType.DECIMAL) {
			master.setDouble(targetField, calcValue.doubleValue());
		}
		
		master.removeValue(EntityHelper.ModifiedBy);
		master.removeValue(EntityHelper.ModifiedOn);
		// TODO 触发器更新的数据不传播
		Application.getCommonService().update(master, false);
	}
}
