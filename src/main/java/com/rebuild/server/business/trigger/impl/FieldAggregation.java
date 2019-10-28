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

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.server.service.bizz.privileges.PrivilegesGuardInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 字段归集可能存在的问题。
 * - 目标记录可能不允许修改（如审批已完成），此时会抛出异常
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/29
 *
 * @see com.rebuild.server.business.trigger.RobotTriggerObserver
 */
public class FieldAggregation implements TriggerAction {
	
	private static final Log LOG = LogFactory.getLog(FieldAggregation.class);

	// 此触发器可能产生连锁反应
	// 如触发器 A 调用 B，而 B 又调用了 C ... 以此类推。此处记录其深度
	private static final ThreadLocal<Integer> CALL_CHAIN_DEPTH = new ThreadLocal<>();
	// 最大调用深度
	private static final int MAX_DEPTH = 5;
	
	final private ActionContext context;

	// 允许无权限更新
	private boolean allowNoPermissionUpdate;
	
	private Entity sourceEntity;
	private Entity targetEntity;
	
	private String followSourceField;
	private ID targetRecordId;

	public FieldAggregation(ActionContext context) {
		this(context, Boolean.TRUE);
	}

	public FieldAggregation(ActionContext context, boolean allowNoPermissionUpdate) {
		this.context = context;
		this.allowNoPermissionUpdate = allowNoPermissionUpdate;
	}
	
	@Override
	public ActionType getType() {
		return ActionType.FIELDAGGREGATION;
	}
	
	@Override
	public boolean isUsableSourceEntity(int entityCode) {
		return true;
	}
	
	@Override
	public void execute(OperatingContext operatingContext) throws TriggerException {
		Integer depth = CALL_CHAIN_DEPTH.get();
		if (depth == null) {
			depth = 0;
		}
		if (depth >= MAX_DEPTH) {
			throw new TriggerException("Too many call-chain with triggers");
		}
		
		this.prepare(operatingContext);
		if (this.targetRecordId == null) {  // 无目标记录
			return;
		}
		
		// 如果当前用户对目标记录无修改权限
		if (!allowNoPermissionUpdate) {
			if (!Application.getSecurityManager().allowed(
					operatingContext.getOperator(), targetRecordId, BizzPermission.UPDATE)) {
				LOG.warn("No privileges to update record of target: " + this.targetRecordId);
				return;
			}
		}
		
		// 更新目标
		Record targetRecord = EntityHelper.forUpdate(targetRecordId, UserService.SYSTEM_USER, false);
		
		JSONArray items = ((JSONObject) context.getActionContent()).getJSONArray("items");
		for (Object o : items) {
			JSONObject item = (JSONObject) o;
			String sourceField = item.getString("sourceField");
			String targetField = item.getString("targetField");
			if (!MetadataHelper.checkAndWarnField(sourceEntity, sourceField)
					|| !MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
				continue;
			}

			// 直接利用SQL计算结果
			String calcMode = item.getString("calcMode");
			String calcField = "COUNT".equalsIgnoreCase(calcMode) ? sourceEntity.getPrimaryField().getName() : sourceField;
			
			String sql = String.format("select %s(%s) from %s where %s = ?", 
					calcMode, calcField, sourceEntity.getName(), followSourceField);
			Object[] result = Application.createQueryNoFilter(sql).setParameter(1, targetRecordId).unique();
			double calcValue = result == null || result[0] == null ? 0d : ObjectUtils.toDouble(result[0]);
			
			DisplayType dt = EasyMeta.getDisplayType(targetEntity.getField(targetField));
			if (dt == DisplayType.NUMBER) {
				targetRecord.setInt(targetField, (int) calcValue);
			} else if (dt == DisplayType.DECIMAL) {
				targetRecord.setDouble(targetField, calcValue);
			}
		}
		
		if (targetRecord.getAvailableFieldIterator().hasNext()) {
			if (allowNoPermissionUpdate) {
				PrivilegesGuardInterceptor.setNoPermissionPassOnce(targetRecordId);
			}

			Application.getEntityService(targetEntity.getEntityCode()).update(targetRecord);
			CALL_CHAIN_DEPTH.set(depth + 1);
		}
	}

	@Override
	public void prepare(OperatingContext operatingContext) throws TriggerException {
		if (sourceEntity != null) {  // 已经初始化
			return;
		}
		
		// FIELD.ENTITY
		String[] targetFieldEntity = ((JSONObject) context.getActionContent()).getString("targetEntity").split("\\.");
		if (!MetadataHelper.containsEntity(targetFieldEntity[1])) {
			return;
		}

		this.sourceEntity = context.getSourceEntity();
		this.targetEntity = MetadataHelper.getEntity(targetFieldEntity[1]);
		this.followSourceField = targetFieldEntity[0];
		if (!sourceEntity.containsField(followSourceField)) {
			return;
		}

		// 找到主记录
		Object[] o = Application.getQueryFactory().uniqueNoFilter(
				context.getSourceRecord(), followSourceField, followSourceField + "." + EntityHelper.OwningUser);
		// o[1] 为空说明记录不存在
		if (o != null && o[0] != null && o[1] != null) {
			this.targetRecordId = (ID) o[0];
		}
	}
}
