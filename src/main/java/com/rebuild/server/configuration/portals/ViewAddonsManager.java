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

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;

import java.util.Collection;

/**
 * 视图-相关项/新建相关
 * 
 * @author devezhao
 * @since 10/22/2018
 */
public class ViewAddonsManager extends BaseLayoutManager {
	
	public static final ViewAddonsManager instance = new ViewAddonsManager();
	private ViewAddonsManager() { }
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public JSON getViewTab(String entity, ID user) {
		JSON tabs = getViewAddons(entity, user, TYPE_TAB);

		// 添加明细实体到第一个
		Entity entityMeta = MetadataHelper.getEntity(entity);
		if (entityMeta.getSlaveEntity() != null) {
			JSON slave = EasyMeta.getEntityShow(entityMeta.getSlaveEntity());
			JSONArray tabsFluent = new JSONArray();
            tabsFluent.add(slave);
            tabsFluent.fluentAddAll((Collection<?>) tabs);
            tabs = tabsFluent;
		}
		return tabs;
	}
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public JSON getViewAdd(String entity, ID user) {
		return getViewAddons(entity, user, TYPE_ADD);
	}
	
	/**
	 * @param entity
	 * @param user
	 * @param applyType
	 * @return
	 */
	private JSON getViewAddons(String entity, ID user, String applyType) {
		final ConfigEntry config = getLayout(user, entity, applyType);
		final Permission useAction = TYPE_TAB.equals(applyType) ? BizzPermission.READ : BizzPermission.CREATE;

		// 未配置则使用全部相关项
		if (config == null) {
		    JSONArray refs = new JSONArray();
			for (Field field : MetadataHelper.getEntity(entity).getReferenceToFields(true)) {
				Entity e = field.getOwnEntity();
				if (e.getMasterEntity() == null &&
                        Application.getSecurityManager().allow(user, e.getEntityCode(), useAction)) {
					refs.add(EasyMeta.getEntityShow(e));
				}
			}
			return refs;
		}

		JSONArray addons = new JSONArray();
		for (Object o : (JSONArray) config.getJSON("config")) {
			String e = (String) o;
			if (MetadataHelper.containsEntity(e)) {
				Entity entityMeta = MetadataHelper.getEntity(e);
				if (Application.getSecurityManager().allow(user, entityMeta.getEntityCode(), useAction)) {
					addons.add(EasyMeta.getEntityShow(entityMeta));
				}
			}
		}
		return addons;
	}
}
