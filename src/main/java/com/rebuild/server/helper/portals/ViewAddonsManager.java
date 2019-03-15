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

package com.rebuild.server.helper.portals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 视图-相关项显示/新建相关
 * 
 * @author devezhao
 * @since 10/22/2018
 */
public class ViewAddonsManager implements PortalsManager {
	
	// 显示相关项
	public static final String TYPE_TAB = "TAB";
	// 新建相关记录
	public static final String TYPE_ADD = "ADD";
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getViewTab(String entity, ID user) {
		JSON tabs = getViewAddons(entity, user, TYPE_TAB);
		
		// 添加明细实体（如有）到第一个
		Entity entityMeta = MetadataHelper.getEntity(entity);
		if (entityMeta.getSlaveEntity() != null) {
			String shows[] = EasyMeta.getEntityShows(entityMeta.getSlaveEntity());
			JSON tabsAll = (JSON) JSON.toJSON(new String[][] { shows });
			((JSONArray) tabsAll).fluentAddAll((Collection<?>) tabs);
			tabs = tabsAll;
		}
		return tabs;
	}
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getViewAdd(String entity, ID user) {
		return getViewAddons(entity, user, TYPE_ADD);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @param type
	 * @return
	 */
	private static JSON getViewAddons(String entity, ID user, String applyType) {
		final Object addons[] = getConfig(entity, applyType);
		final Permission RoC = TYPE_TAB.equals(applyType) ? BizzPermission.READ : BizzPermission.CREATE;
		
		// 未配置则使用全部相关项
		if (addons == null) {
			Entity entityMeta = MetadataHelper.getEntity(entity);
			
			Set<String[]> refs = new HashSet<>();
			for (Field field : entityMeta.getReferenceToFields()) {
				Entity e = field.getOwnEntity();
				// 过滤明细实体
				if (e.getMasterEntity() != null) {
					continue;
				}
				if (Application.getSecurityManager().allowed(user, e.getEntityCode(), RoC)) {
					refs.add(EasyMeta.getEntityShows(e));
				}
			}
			return (JSON) JSONArray.toJSON(refs);
		}
		
		List<String[]> configured = new ArrayList<>();
		for (Object o : (JSONArray) addons[1]) {
			String e = (String) o;
			if (MetadataHelper.containsEntity(e)) {
				Entity entityMeta = MetadataHelper.getEntity(e);
				if (Application.getSecurityManager().allowed(user, entityMeta.getEntityCode(), RoC)) {
					configured.add(EasyMeta.getEntityShows(entityMeta));
				}
			}
		}
		return (JSON) JSON.toJSON(configured);
	}
	
	/**
	 * @param entity
	 * @param applyType
	 * @return
	 */
	public static Object[] getConfig(String entity, String applyType) {
		Object[] config = Application.createQueryNoFilter(
				"select configId,config from ViewAddonsConfig where belongEntity = ? and applyType = ?")
				.setParameter(1, entity)
				.setParameter(2, applyType)
				.unique();
		if (config != null) {
			config[1] = JSON.parseArray((String) config[1]);
		}
		return config;
	}
}
