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
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.engine.ID;

/**
 * 高级过滤器
 * 
 * @author devezhao
 * @since 09/30/2018
 */
public class AdvFilterManager extends SharableManager<ID> {
	
	public static final AdvFilterManager instance = new AdvFilterManager();
	private AdvFilterManager() { }
	
	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public JSONArray getAdvFilterList(String entity, ID user) {
		ConfigEntry[] entries = getAdvFilterListRaw(entity, user);
		return JSONUtils.toJSONArray(entries);
	}
	
	/**
	 * 获取高级查询列表
	 * 
	 * @param entity
	 * @param user
	 * @return
	 */
	protected ConfigEntry[] getAdvFilterListRaw(String entity, ID user) {
		final String ckey = "AdvFilter-" + entity;
		ConfigEntry[] entries = (ConfigEntry[]) Application.getCommonCache().getx(ckey);
		if (entries == null) {
			Object[][] array = Application.createQueryNoFilter(
					"select configId,filterName,shareTo,createdBy from FilterConfig where belongEntity = ? order by filterName")
					.setParameter(1, entity)
					.array();
			List<ConfigEntry> list = new ArrayList<>();
			for (Object[] o : array) {
				ConfigEntry e = new ConfigEntry()
						.set("id", o[0])
						.set("name", o[1])
						.set("shareTo", o[2])
						.set("createdBy", o[3]);
				list.add(e);
			}
			
			entries = list.toArray(new ConfigEntry[list.size()]);
			Application.getCommonCache().putx(ckey, entries);
		}
		
		final boolean isAdmin = UserHelper.isAdmin(user);
		List<ConfigEntry> ret = new ArrayList<>();
		for (ConfigEntry e : entries) {
			ID createdBy = e.getID("createdBy");
			String shareTo = e.getString("shareTo");
			ConfigEntry clone = e.clone();
			clone.set("createdBy", null);
			clone.set("shareTo", null);
			
			if (UserHelper.isAdmin(createdBy)) {
				if (isAdmin) {
					clone.set("editable", true);
					ret.add(clone);
				} else if (SHARE_ALL.equalsIgnoreCase(shareTo)) {
					ret.add(clone);
				}
			} else if (createdBy.equals(user)) {
				clone.set("editable", true);
				ret.add(clone);
			}
		}
		return ret.toArray(new ConfigEntry[ret.size()]);
	}
	
	/**
	 * 获取高级查询
	 * 
	 * @param configId
	 * @return
	 */
	public ConfigEntry getAdvFilter(ID configId) {
		final String ckey = "AdvFilterDATA-" + configId;
		ConfigEntry filter = (ConfigEntry) Application.getCommonCache().getx(ckey);
		if (filter != null) {
			return filter.clone();
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select configId,config,filterName,shareTo from FilterConfig where configId = ?")
				.setParameter(1, configId)
				.unique();
		if (o == null) {
			return null;
		}
		
		filter = new ConfigEntry()
				.set("id", configId)
				.set("filter", JSON.parseObject((String) o[1]))
				.set("name", o[2])
				.set("shareTo", o[3]);
		Application.getCommonCache().putx(ckey, filter);
		return filter.clone();
	}
	
	@Override
	public void clean(ID cacheKey) {
		Application.getCommonCache().evict("AdvFilterDATA-" + cacheKey);
		
		Object[] o = Application.createQueryNoFilter(
				"select belongEntity from FilterConfig where configId = ?")
				.setParameter(1, cacheKey)
				.unique();
		if (o != null) {
			Application.getCommonCache().evict("AdvFilter-" + o[0]);
		}
	}
}