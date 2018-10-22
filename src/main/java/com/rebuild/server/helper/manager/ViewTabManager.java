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

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * 视图标签页
 * 
 * @author devezhao
 * @since 10/22/2018
 */
public class ViewTabManager {
	
	/**
	 * @param entity
	 * @return
	 */
	public static JSON getViewTab(String entity) {
		Object vtab[] = getRaw(entity);
		if (vtab == null) {
			Entity entityMeta = MetadataHelper.getEntity(entity);
			Set<String[]> refTos = new HashSet<>();
			for (Field field : entityMeta.getReferenceToFields()) {
				Entity e = field.getOwnEntity();
				refTos.add(new String[] { e.getName(), EasyMeta.getLabel(e) });
			}
			return (JSON) JSONArray.toJSON(refTos);
		}
		
		JSONArray config = (JSONArray) vtab[1];
		List<String[]> configWarp = new ArrayList<>();
		for (Object o : config) {
			String e = (String) o;
			if (MetadataHelper.containsEntity(e)) {
				configWarp.add(new String[] { e, EasyMeta.getLabel(MetadataHelper.getEntity(e)) });
			}
		}
		return (JSON) JSON.toJSON(configWarp);
	}
	/**
	 * @param entity
	 * @param field
	 * @param isAll
	 * @param reload
	 * @return
	 */
	public static Object[] getRaw(String entity) {
		Object[] vtab = Application.createQueryNoFilter(
				"select viewTabId,config from ViewTabConfig where belongEntity = ?")
				.setParameter(1, entity)
				.unique();
		if (vtab != null) {
			vtab[1] = JSON.parseArray((String) vtab[1]);
		}
		return vtab;
	}
}
