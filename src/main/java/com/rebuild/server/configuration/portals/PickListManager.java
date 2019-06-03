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

import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ConfigManager;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 列表项
 * 
 * @author zhaofang123@gmail.com
 * @since 09/06/2018
 */
public class PickListManager implements ConfigManager<Object> {

	public static final PickListManager instance = new PickListManager();
	private PickListManager() { }
	
	/**
	 * @param field
	 * @return
	 */
	public JSONArray getPickList(Field field) {
		return getPickList(field, false);
	}
	
	/**
	 * @param field
	 * @param includeHide
	 * @return
	 */
	public JSONArray getPickList(Field field, boolean includeHide) {
		ConfigEntry entries[] = getPickListRaw(field.getOwnEntity().getName(), field.getName(), includeHide);
		JSONArray ret = new JSONArray();
		for (ConfigEntry e : entries) {
			ret.add(e.toJSON());
		}
		return ret;
	}
	
	/**
	 * @param entity
	 * @param field
	 * @param includeHide
	 * @return
	 */
	public ConfigEntry[] getPickListRaw(String entity, String field, boolean includeHide) {
		final String ckey = String.format("PickList-%s.%s", entity, field);
		ConfigEntry[] entries = (ConfigEntry[]) Application.getCommonCache().getx(ckey);
		if (entries == null) {
			Object[][] array = Application.createQueryNoFilter(
					"select itemId,text,isDefault,isHide from PickList where belongEntity = ? and belongField = ? order by seq asc")
					.setParameter(1, entity)
					.setParameter(2, field)
					.array();
			List<ConfigEntry> list = new ArrayList<>();
			for (Object[] o : array) {
				ConfigEntry entry = new ConfigEntry()
						.set("id", o[0])
						.set("text", o[1])
						.set("default", o[2])
						.set("hide", o[3]);
				list.add(entry);
			}
			
			entries = list.toArray(new ConfigEntry[list.size()]);
			Application.getCommonCache().putx(ckey, entries);
		}
		
		List<ConfigEntry> ret = new ArrayList<>();
		for (ConfigEntry entry : entries) {
			if (includeHide || !entry.getBoolean("hide")) {
				ret.add(entry.clone());
			}
		}
		return ret.toArray(new ConfigEntry[ret.size()]);
	}
	
	/**
	 * @param itemId
	 * @return
	 */
	public String getLabel(ID itemId) {
		final String ckey = "PickListLABEL-" + itemId;
		String cval = Application.getCommonCache().get(ckey);
		if (cval != null) {
			return cval;
		}
		
		Object[] o = Application.createQueryNoFilter(
				"select text from PickList where itemId = ?")
				.setParameter(1, itemId)
				.unique();
		if (o != null) {
			cval = (String) o[0];
		}
		Application.getCommonCache().put(ckey, cval);
		return cval;
	}
	
	/**
	 * @param label
	 * @param field
	 * @return
	 */
	public ID findItemByLabel(String label, Field field) {
		Object[] o = Application.createQueryNoFilter(
				"select itemId from PickList where belongEntity = ? and belongField = ? and text = ?")
				.setParameter(1, field.getOwnEntity().getName())
				.setParameter(2, field.getName())
				.setParameter(3, label)
				.unique();
		return o == null ? null : (ID) o[0];
	}
	
	@Override
	public void clean(Object cacheKey) {
		if (cacheKey instanceof ID) {
			Application.getCommonCache().evict("PickListLABEL-" + cacheKey);
		} else if (cacheKey instanceof Field) {
			Field field = (Field) cacheKey;
			Application.getCommonCache().evict(String.format("PickList-%s.%s", field.getOwnEntity().getName(), field.getName()));
		}
	}
}
