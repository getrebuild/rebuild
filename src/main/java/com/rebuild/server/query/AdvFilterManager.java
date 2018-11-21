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

package com.rebuild.server.query;

import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.helper.manager.LayoutManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager {
	
	private static final Log LOG = LogFactory.getLog(AdvFilterManager.class);
	
	/**
	 * 快速查询过滤条件特别名称
	 */
	public static final String FILTER_QUICK = "$QUICK$";

	/**
	 * @param entity
	 * @param user
	 * @return
	 * @see LayoutManager#getLayoutConfigRaw(String, String, ID)
	 */
	public static Object[] getQuickFilterRaw(String entity, ID user) {
		Assert.notNull(entity, "[entity] not be null");
		Assert.notNull(user, "[user] not be null");
		
		String sql = "select filterId,config,modifiedOn from FilterConfig where belongEntity = '%s' and filterName = '%s' and applyTo = ?";
		sql = String.format(sql, entity, FILTER_QUICK);
		
		Object[] myself = Application.createQueryNoFilter(sql + " and createdBy = ?")
				.setParameter(1, LayoutManager.APPLY_SELF)
				.setParameter(2, user)
				.unique();
		Object[] global = Application.createQueryNoFilter(sql).setParameter(1, LayoutManager.APPLY_ALL).unique();
		
		Object[] cfgs = global;
		
		// 使用最近更新的
		if (myself != null && global != null) {
			cfgs = ((Date) myself[2]).getTime() > ((Date) global[2]).getTime() ? myself : global;
		} else if (myself != null) {
			cfgs = myself;
		}
		
		if (cfgs == null) {
			Entity entityMeta = MetadataHelper.getEntity(entity);
			Field namedField = entityMeta.getNameField();
			if (allowedQuickFilter(namedField)) {
				cfgs = new Object[] { null, null };
				cfgs[1] = String.format("{items:[{ op:'lk', field:'%s', value:'{1}' }]}", namedField.getName());
			} else {
				return null;
			}
		}
		
		Entity metaEntity = MetadataHelper.getEntity(entity);
		JSONObject config = (JSONObject) JSON.parse((String) cfgs[1]);
		JSONArray items = config.getJSONArray("items");
		for (Iterator<Object> iter = items.iterator(); iter.hasNext(); ) {
			JSONObject item = (JSONObject) iter.next();
			String field = item.getString("field");
			if (!metaEntity.containsField(field)) {
				LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
				continue;
			}
			
			String label = EasyMeta.getLabel(metaEntity.getField(field));
			item.put("label", label);
		}
		cfgs[1] = config;
		return cfgs;
	}
	
	/**
	 * 查找配置ID
	 * 
	 * @param cfgid
	 * @param toAll
	 * @param entity
	 * @param user
	 * @return
	 * @see LayoutManager#detectConfigId(ID, boolean, String, String, ID)
	 */
	public static ID detectQuickConfigId(ID cfgid, boolean toAll, String entity, ID user) {
		String sql = "select filterId from FilterConfig where belongEntity = '%s' and filterName = '%s'";
		sql = String.format(sql, entity, FILTER_QUICK);
		
		boolean isAdmin = Application.getUserStore().getUser(user).isAdmin();
		// 管理员有两种配置，一个全局一个自己
		if (isAdmin) {
			sql += " and applyTo = '%s'";
			sql = String.format(sql, toAll ? LayoutManager.APPLY_ALL : LayoutManager.APPLY_SELF);
		} else {
			sql += " and applyTo = '%s' and createdBy = '%s'";
			sql = String.format(sql, LayoutManager.APPLY_SELF, user.toLiteral());
		}
		
		Object[] detect = Application.createQueryNoFilter(sql).unique();
		return detect == null ? null : (ID) detect[0];
	}
	
	/**
	 * 是否允许 QuickFilter 字段
	 * 
	 * @param field
	 * @return
	 */
	public static boolean allowedQuickFilter(Field field) {
		if (field == null) {
			return false;
		}
		if (MetadataSorter.isFilterField(field)) {
			return false;
		}
		
		DisplayType dt = EasyMeta.getDisplayType(field);
		return (dt == DisplayType.TEXT || dt == DisplayType.URL || dt == DisplayType.EMAIL || dt == DisplayType.PHONE || dt == DisplayType.PICKLIST);
	}
	
	// --
	
	/**
	 * 获取高级查询列表
	 * 
	 * @param entity
	 * @param user
	 * @return
	 */
	public static Object[][] getAdvFilterList(String entity, ID user) {
		Assert.notNull(entity, "[entity] not be null");
		Assert.notNull(user, "[user] not be null");
		
		Object[][] array = Application.createQueryNoFilter(
				"select filterId,filterName,createdBy from FilterConfig"
				+ " where belongEntity = ? and filterName <> ? and ((applyTo = 'SELF' and createdBy = ?) or applyTo = 'ALL')"
				+ " order by filterName")
				.setParameter(1, entity)
				.setParameter(2, FILTER_QUICK)
				.setParameter(3, user)
				.array();
		for (Object[] o : array) {
			o[2] = o[2].equals(user);  // allow edit?
		}
		return array;
	}
	
	/**
	 * 获取高级查询列表
	 * 
	 * @param filterId
	 * @return
	 */
	public static Object[] getAdvFilterRaw(ID filterId) {
		Assert.notNull(filterId, "[filterId] not be null");
		Object[] filter = Application.createQueryNoFilter(
				"select filterId,config,filterName,applyTo,createdBy from FilterConfig where filterId = ?")
				.setParameter(1, filterId)
				.unique();
		if (filter == null) {
			return null;
		}
		
		String cfg = (String) filter[1];
		filter[1] = JSON.parseObject(cfg);
		return filter;
	}
}