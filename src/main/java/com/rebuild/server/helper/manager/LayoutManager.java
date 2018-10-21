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

package com.rebuild.server.helper.manager;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.engine.ID;

/**
 * 布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 09/15/2018
 */
public class LayoutManager {
	
	protected static final Log LOG = LogFactory.getLog(LayoutManager.class);
	
	// 表单
	public static final String TYPE_FORM = "FORM";
	// 数据列表
	public static final String TYPE_DATALIST = "DATALIST";
	// 导航
	public static final String TYPE_NAVI = "NAVI";

	// 私有配置
	public static final String APPLY_SELF = "SELF";
	// 全局配置
	public static final String APPLY_ALL = "ALL";
	
	/**
	 * 获取配置
	 * 
	 * @param entity
	 * @param type
	 * @param user
	 * @return
	 */
	public static Object[] getLayoutConfigRaw(String entity, String type, ID user) {
		Assert.notNull(entity, "[entity] not be null");
		Assert.notNull(type, "[type] not be null");
		Assert.notNull(user, "[user] not be null");
		
		String sql = "select layoutId,config,modifiedOn from LayoutConfig where belongEntity = '%s' and type = '%s' and applyTo = ?";
		sql = String.format(sql, entity, type);

		Object[] myself = Application.createQueryNoFilter(sql + " and createdBy = ?")
				.setParameter(1, APPLY_SELF)
				.setParameter(2, user)
				.unique();
		Object[] global = Application.createQueryNoFilter(sql).setParameter(1, APPLY_ALL).unique();

		Object[] cfgs = global;
		
		// 使用最近更新的
		if (myself != null && global != null) {
			cfgs = ((Date) myself[2]).getTime() > ((Date) global[2]).getTime() ? myself : global;
		} else if (myself != null) {
			cfgs = myself;
		}
		
		if (cfgs == null) {
			return null;
		}
		cfgs[1] = JSON.parse((String) cfgs[1]);
		cfgs[2] = CalendarUtils.getUTCDateTimeFormat().format(cfgs[2]);
		return cfgs;
	}
	
	/**
	 * 查找配置ID
	 * 
	 * @param cfgid
	 * @param toAll
	 * @param entity
	 * @param type
	 * @param user
	 * @return
	 */
	public static ID detectConfigId(ID cfgid, boolean toAll, String entity, String type, ID user) {
		String sql = "select layoutId from LayoutConfig where belongEntity = '%s' and type = '%s'";
		sql = String.format(sql, entity, type);
		
		boolean isAdmin = Application.getUserStore().getUser(user).isAdmin();
		// 管理员有两种配置，一个全局一个自己
		if (isAdmin) {
			sql += " and applyTo = '%s'";
			sql = String.format(sql, toAll ? APPLY_ALL : APPLY_SELF);
		} else {
			sql += " and applyTo = '%s' and createdBy = '%s'";
			sql = String.format(sql, APPLY_SELF, user.toLiteral());
		}
		
		Object[] detect = Application.createQueryNoFilter(sql).unique();
		return detect == null ? null : (ID) detect[0];
	}
	
	/**
	 * TODO 清理配置缓存
	 * 
	 * @param entity
	 * @param type
	 */
	public static void cleanLayoutConfig(String entity, String type, ID user) {
	}
}
