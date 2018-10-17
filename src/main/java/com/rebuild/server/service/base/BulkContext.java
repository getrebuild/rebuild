/*
rebuild - Building your system freely.
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

import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
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
	// 待操作过滤条件（通过条件确定记录）
	private JSONObject filterExp;
	
	// 级联操作实体
	private String cascades[];
	
	private Entity mainEntity;

	/**
	 * @param opUser
	 * @param action
	 * @param toUser
	 * @param cascades
	 * @param records
	 */
	public BulkContext(ID opUser, Permission action, ID toUser, String cascades[], ID[] records) {
		Assert.isTrue(records.length <= 200, "最多允许操作200条记录");
		this.opUser = opUser;
		this.action = action;
		this.toUser = toUser;
		this.cascades = cascades;
		this.records = records;
	}
	
	/**
	 * @param opUser
	 * @param action
	 * @param toUser
	 * @param cascades
	 * @param filterExp
	 */
	public BulkContext(ID opUser, Permission action, ID toUser, String cascades[], JSONObject filterExp) {
		this.opUser = opUser;
		this.action = action;
		this.toUser = toUser;
		this.cascades = cascades;
		this.filterExp = filterExp;
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
	
	public Entity getMainEntity() {
		if (mainEntity != null) {
			return mainEntity;
		}
		
		if (records != null) {
			mainEntity = MetadataHelper.getEntity(records[0].getEntityCode());
		} else {
			mainEntity = MetadataHelper.getEntity(filterExp.getString("entity"));
		}
		return mainEntity;
	}
}
