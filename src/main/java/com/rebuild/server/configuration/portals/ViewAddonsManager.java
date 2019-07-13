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

package com.rebuild.server.configuration.portals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;

/**
 * 视图-相关项显示/新建相关
 * 
 * @author devezhao
 * @since 10/22/2018
 */
public class ViewAddonsManager extends BaseLayoutManager {
	
	public static final ViewAddonsManager instance = new ViewAddonsManager();
	private ViewAddonsManager() { }
	
	/**
	 * @param belongEntity
	 * @param user
	 * @return
	 */
	public JSON getViewTab(String belongEntity, ID user) {
		JSON tabs = getViewAddons(belongEntity, user, TYPE_TAB);
		
		// 添加明细实体（如有）到第一个
		Entity entityMeta = MetadataHelper.getEntity(belongEntity);
		if (entityMeta.getSlaveEntity() != null) {
			String shows[] = EasyMeta.getEntityShow(entityMeta.getSlaveEntity());
			JSON tabsAll = (JSON) JSON.toJSON(new String[][] { shows });
			((JSONArray) tabsAll).fluentAddAll((Collection<?>) tabs);
			tabs = tabsAll;
		}
		return tabs;
	}
	
	/**
	 * @param belongEntity
	 * @param user
	 * @return
	 */
	public JSON getViewAdd(String belongEntity, ID user) {
		return getViewAddons(belongEntity, user, TYPE_ADD);
	}
	
	/**
	 * @param belongEntity
	 * @param user
	 * @param applyType
	 * @return
	 */
	private JSON getViewAddons(String belongEntity, ID user, String applyType) {
		final ConfigEntry config = getLayoutConfig(user, belongEntity, applyType);
		final Permission useAction = TYPE_TAB.equals(applyType) ? BizzPermission.READ : BizzPermission.CREATE;
		
		// 未配置则使用全部相关项
		if (config == null) {
			Set<String[]> refs = new HashSet<>();
			for (Field field : MetadataHelper.getEntity(belongEntity).getReferenceToFields()) {
				if (isFilter(field)) {
					continue;
				}
				
				Entity e = field.getOwnEntity();
				if (Application.getSecurityManager().allowed(user, e.getEntityCode(), useAction)) {
					refs.add(EasyMeta.getEntityShow(e));
				}
			}
			return (JSON) JSONArray.toJSON(refs);
		}
		
		List<String[]> addons = new ArrayList<>();
		for (Object o : (JSONArray) config.getJSON("config")) {
			String e = (String) o;
			if (MetadataHelper.containsEntity(e)) {
				Entity entityMeta = MetadataHelper.getEntity(e);
				if (Application.getSecurityManager().allowed(user, entityMeta.getEntityCode(), useAction)) {
					addons.add(EasyMeta.getEntityShow(entityMeta));
				}
			}
		}
		return (JSON) JSON.toJSON(addons);
	}
	
	@Override
	protected boolean isSingleConfig() {
		return true;
	}
	
	/**
	 * 是否过滤此字段（引用的实体）
	 * 
	 * @param refField
	 * @return
	 */
	public boolean isFilter(Field refField) {
		// 过滤任意引用
		if (refField.getType() == FieldType.ANY_REFERENCE) {
			return true;
		}
		// 过滤明细实体
		if (refField.getOwnEntity().getMasterEntity() != null) {
			return true;
		}
		return false;
	}
}
