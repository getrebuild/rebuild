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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 视图-相关项
 * 
 * @author devezhao
 * @since 10/22/2018
 */
public class ViewFeatManager {
	
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
		return getViewFeat(entity, TYPE_TAB, user);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getViewAdd(String entity, ID user) {
		return getViewFeat(entity, TYPE_ADD, user);
	}
	
	/**
	 * @param entity
	 * @param type
	 * @param user
	 * @return
	 */
	protected static JSON getViewFeat(String entity, String type, ID user) {
		final Object FEAT[] = getRaw(entity, type);
		final Permission RoC = TYPE_TAB.equals(type) ? BizzPermission.READ : BizzPermission.CREATE;
		
		// TODO 未配置则使用全部相关项 ???
		if (FEAT == null) {
			Entity entityMeta = MetadataHelper.getEntity(entity);
			Set<String[]> refs = new HashSet<>();
			for (Field field : entityMeta.getReferenceToFields()) {
				Entity e = field.getOwnEntity();
				if (Application.getSecurityManager().allowed(user, e.getEntityCode(), RoC)) {
					EasyMeta easyMeta = new EasyMeta(e);
					refs.add(new String[] { easyMeta.getName(), easyMeta.getLabel(), easyMeta.getIcon() });
				}
			}
			return (JSON) JSONArray.toJSON(refs);
		}
		
		List<String[]> configured = new ArrayList<>();
		for (Object o : (JSONArray) FEAT[1]) {
			String e = (String) o;
			if (MetadataHelper.containsEntity(e)) {
				Entity entityMeta = MetadataHelper.getEntity(e);
				if (Application.getSecurityManager().allowed(user, entityMeta.getEntityCode(), RoC)) {
					EasyMeta easyMeta = new EasyMeta(entityMeta);
					configured.add(new String[] { easyMeta.getName(), easyMeta.getLabel(), easyMeta.getIcon() });
				}
			}
		}
		return (JSON) JSON.toJSON(configured);
	}
	
	/**
	 * @param entity
	 * @param type
	 * @return
	 */
	public static Object[] getRaw(String entity, String type) {
		Object[] feat = Application.createQueryNoFilter(
				"select featId,config from ViewFeatConfig where belongEntity = ? and type = ?")
				.setParameter(1, entity)
				.setParameter(2, type)
				.unique();
		if (feat != null) {
			feat[1] = JSON.parseArray((String) feat[1]);
		}
		return feat;
	}
}
