/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.ConfigManager;
import com.rebuild.utils.JSONUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表项
 * 
 * @author zhaofang123@gmail.com
 * @since 09/06/2018
 */
public class PickListManager implements ConfigManager {

	public static final PickListManager instance = new PickListManager();
	protected PickListManager() { }

	/**
	 * @param field
	 * @return
	 */
	public JSONArray getPickList(Field field) {
		ConfigEntry[] entries = getPickListRaw(field, false);
		for (ConfigEntry e : entries) {
			e.set("hide", null);
			e.set("mask", null);
		}
		return JSONUtils.toJSONArray(entries);
	}
	
	/**
	 * @param field
	 * @param includeHide
	 * @return
	 */
	public JSONArray getPickList(Field field, boolean includeHide) {
		ConfigEntry[] entries = getPickListRaw(field, includeHide);
		return JSONUtils.toJSONArray(entries);
	}

	/**
	 * @param field
	 * @param includeHide
	 * @return
	 */
	public ConfigEntry[] getPickListRaw(Field field, boolean includeHide) {
		return getPickListRaw(field.getOwnEntity().getName(), field.getName(), includeHide);
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
					"select itemId,text,isDefault,isHide,maskValue from PickList where belongEntity = ? and belongField = ? order by seq asc")
					.setParameter(1, entity)
					.setParameter(2, field)
					.array();
			List<ConfigEntry> list = new ArrayList<>();
			for (Object[] o : array) {
				ConfigEntry entry = new ConfigEntry()
						.set("id", o[0])
						.set("text", o[1])
						.set("default", o[2])
						.set("hide", o[3])
						.set("mask", o[4]);
				list.add(entry);
			}
			
			entries = list.toArray(new ConfigEntry[0]);
			Application.getCommonCache().putx(ckey, entries);
		}
		
		List<ConfigEntry> ret = new ArrayList<>();
		for (ConfigEntry entry : entries) {
			if (includeHide || !entry.getBoolean("hide")) {
				ret.add(entry.clone());
			}
		}
		return ret.toArray(new ConfigEntry[0]);
	}
	
	/**
	 * @param itemId
	 * @return
	 */
	public String getLabel(ID itemId) {
		final String ckey = "PickListLABEL-" + itemId;
		String cached = Application.getCommonCache().get(ckey);
		if (cached != null) {
			return cached.equals(DELETED_ITEM) ? null : cached;
		}

		Object[] o = Application.createQueryNoFilter(
				"select text from PickList where itemId = ?")
				.setParameter(1, itemId)
				.unique();
		if (o != null) cached = (String) o[0];
		// 可能已删除
		if (cached == null) cached = DELETED_ITEM;

		Application.getCommonCache().put(ckey, cached);
		return cached.equals(DELETED_ITEM) ? null : cached;
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

	/**
	 * 获取默认项
	 *
	 * @param field
	 * @return
	 */
	public ID getDefaultItem(Field field) {
		for (ConfigEntry e : getPickListRaw(field, false)) {
			if (e.getBoolean("default")) {
				return e.getID("id");
			}
		}
		return null;
	}
	
	@Override
	public void clean(Object idOrField) {
		if (idOrField instanceof ID) {
			Application.getCommonCache().evict("PickListLABEL-" + idOrField);
		} else if (idOrField instanceof Field) {
			Field field = (Field) idOrField;
			Application.getCommonCache().evict(String.format("PickList-%s.%s", field.getOwnEntity().getName(), field.getName()));
		}
	}
}
