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

import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单布局管理
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class FormsManager extends BaseLayoutManager<Entity> {

	public static final FormsManager instance = new FormsManager();
	protected FormsManager() { }
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public ConfigEntry getFormLayout(String entity, ID user) {
		final String ckey = "FormsManager-" + entity;
		ConfigEntry entry = (ConfigEntry) Application.getCommonCache().getx(ckey);
		if (entry != null) {
			return entry.clone();
		}
		
		entry = new ConfigEntry()
				.set("entity", entity);
		Object[] raw = getLayoutOfForm(user, entity);
		if (raw != null) {
			entry.set("elements", JSONUtils.EMPTY_ARRAY)
					.set("id", raw[0]);
		} else {
			entry.set("elements", JSONUtils.EMPTY_ARRAY);
		}
		Application.getCommonCache().putx(ckey, entry);
		return entry.clone();
	}
	
	@Override
	protected boolean isSingleConfig() {
		return true;
	}
	
	@Override
	public void clean(Entity cacheKey) {
		Application.getCommonCache().evict("FormsManager-" + cacheKey.getName());
	}
}
