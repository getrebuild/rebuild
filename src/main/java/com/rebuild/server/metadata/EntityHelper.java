/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.record.FieldValueException;
import cn.devezhao.persist4j.record.JsonRecordCreator;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * @author Zhao Fangfang
 * @since 1.0, 2013-6-26
 * @see MetadataHelper
 */
public class EntityHelper {
	
	/**
	 * 实体是否具有权限字段
	 * 
	 * @param entity
	 * @return
	 * @see MetadataHelper#hasPrivilegesField(Entity)
	 */
	public static boolean hasPrivilegesField(Entity entity) {
		return MetadataHelper.hasPrivilegesField(entity);
	}
	
	/**
	 * @param data
	 * @param user
	 * @return
	 */
	public static Record parse(JSONObject data, ID user) {
		JSONObject metadata = data.getJSONObject(JsonRecordCreator.META_FIELD);
		if (metadata == null) {
			throw new FieldValueException("无效实体数据格式(1): " + data.toJSONString());
		}
		String entityName = metadata.getString("entity");
		if (StringUtils.isBlank(entityName)) {
			throw new FieldValueException("无效实体数据格式(2): " + data.toJSONString());
		}

		ExtRecordCreator creator = new ExtRecordCreator(MetadataHelper.getEntity(entityName), data, user);
		Record record = creator.create(false);
		ExtRecordCreator.bindCommonsFieldsValue(record, record.getPrimary() == null);
		return record;
	}

	/**
	 * @param recordId
	 * @param user
	 * @return
	 */
	public static Record forUpdate(ID recordId, ID user) {
		return forUpdate(recordId, user, true);
	}
	
	/**
	 * @param recordId
	 * @param user
	 * @param bindCommons 是否自动补充公共字段
	 * @return
	 */
	public static Record forUpdate(ID recordId, ID user, boolean bindCommons) {
		Assert.notNull(recordId, "[recordId] not be bull");
		Assert.notNull(recordId, "[user] not be bull");
		
		Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
		Record record = new StandardRecord(entity, user);
		record.setID(entity.getPrimaryField().getName(), recordId);
		if (bindCommons) {
			ExtRecordCreator.bindCommonsFieldsValue(record, false);
		}
		return record;
	}
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static Record forNew(int entity, ID user) {
		return forNew(MetadataHelper.getEntity(entity), user);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static Record forNew(String entity, ID user) {
		return forNew(MetadataHelper.getEntity(entity), user);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	private static Record forNew(Entity entity, ID user) {
		Assert.notNull(user, "[user] not be bull");
		Record record = new StandardRecord(entity, user);
		ExtRecordCreator.bindCommonsFieldsValue(record, true);
		return record;
	}
	
	// 公共字段/保留字段
	
	public static final String CreatedOn = "createdOn";
	public static final String CreatedBy = "createdBy";
	public static final String ModifiedOn = "modifiedOn";
	public static final String ModifiedBy = "modifiedBy";
	public static final String OwningUser = "owningUser";
	public static final String OwningDept = "owningDept";
	
	public static final String AutoId = "autoId";
	public static final String QuickCode = "quickCode";
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	public static final String IsDeleted = "isDeleted";

	public static final String ApprovalId = "approvalId";
	public static final String ApprovalState = "approvalState";
	public static final String ApprovalStepNode = "approvalStepNode";
	
	// 系统实体

	public static final int User = 1;
	public static final int Department = 2;
	public static final int Role = 3;
	public static final int RolePrivileges = 4;
	public static final int RoleMember = 5;

	public static final int MetaEntity = 10;
	public static final int MetaField = 11;
	public static final int PickList = 12;
	public static final int LayoutConfig = 13;
	public static final int FilterConfig = 14;
	public static final int DashboardConfig = 16;
	public static final int ChartConfig = 17;
	public static final int Classification = 18;
	public static final int ClassificationData = 19;
	public static final int ShareAccess = 20;
	public static final int SystemConfig = 21;
	public static final int Notification = 22;
	public static final int Attachment = 23;
	public static final int AttachmentFolder = 24;
	public static final int LoginLog = 25;
	public static final int AutoFillinConfig = 26;
	public static final int RobotTriggerConfig = 27;
	public static final int RobotApprovalConfig = 28;
	public static final int RobotApprovalStep = 29;

	public static final int RebuildApi = 30;
	public static final int RebuildApiRequest = 31;

	public static final int DataReportConfig = 32;

	public static final int RecycleBin = 33;
	public static final int RevisionHistory = 34;

}
