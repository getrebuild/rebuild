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

package com.rebuild.server.service.base;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.RebuildException;
import com.rebuild.server.metadata.MetadataHelper;
import org.apache.commons.lang.ArrayUtils;

/**
 * 批量操作上下文
 * 
 * @author devezhao
 * @since 10/17/2018
 */
public class BulkContext {
	
	// 操作用户
	private ID opUser;
	// 执行动作
	private Permission action;
	// [目标用户]
	private ID toUser;
	// 待操作记录
	private ID[] records;
	// 待操作记录（通过过滤条件获得）
	private JSONObject filterExp;
	// [待操作记录所依附的主记录]
	private ID targetRecord;
	// [级联操作实体]
	private String[] cascades;
	
	final private Entity mainEntity;
	
	/**
	 * @param opUser
	 * @param action
	 * @param toUser
	 * @param cascades
	 * @param records
	 * @param filterExp
	 * @param recordMaster
	 */
	private BulkContext(ID opUser, Permission action, ID toUser, String[] cascades, ID[] records, JSONObject filterExp, ID recordMaster) {
		this.opUser = opUser;
		this.action = action;
		this.toUser = toUser;
		this.records = records;
		this.filterExp = filterExp;
		this.targetRecord = recordMaster;
		this.cascades = cascades;
		this.mainEntity = detecteMainEntity();
	}

	/**
	 * 有目标用户的，如分派/共享/删除
	 * 
	 * @param opUser
	 * @param action
	 * @param toUser
	 * @param cascades
	 * @param records
	 */
	public BulkContext(ID opUser, Permission action, ID toUser, String[] cascades, ID[] records) {
		this(opUser, action, toUser, cascades, records, null, null);
	}
	
	/**
	 * 无目标用户的，如取消共享
	 * 
	 * @param opUser
	 * @param action
	 * @param records
	 * @param targetRecord
	 */
	public BulkContext(ID opUser, Permission action, ID[] records, ID targetRecord) {
		this(opUser, action, null, null, records, null, targetRecord);
	}

	public ID getOpUser() {
		return opUser;
	}

	public Permission getAction() {
		return action;
	}

	public ID getToUser() {
		return toUser;
	}
	
	public String[] getCascades() {
		return cascades == null ? ArrayUtils.EMPTY_STRING_ARRAY : cascades;
	}
	
	public ID[] getRecords() {
		return records;
	}
	
	public JSONObject getFilterExp() {
		return filterExp;
	}

	public ID getTargetRecord() {
		return targetRecord;
	}
	
	public Entity getMainEntity() {
		return mainEntity;
	}
	
	private Entity detecteMainEntity() {
		if (targetRecord != null) {
			return MetadataHelper.getEntity(targetRecord.getEntityCode());
		} else if (records != null && records.length > 0) {
			return MetadataHelper.getEntity(records[0].getEntityCode());
		} else if (filterExp != null) {
			return MetadataHelper.getEntity(filterExp.getString("entity"));
		}
		throw new RebuildException("No record for operate");
	}
}
