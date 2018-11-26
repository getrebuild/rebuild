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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 数据列表
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class DataListManager extends LayoutManager {

	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getColumnLayout(String entity, ID user) {
		Object[] cfgs = getLayoutConfigRaw(entity, TYPE_DATALIST, user);
		
		List<Map<String, Object>> columnList = new ArrayList<>();
		Entity entityMeta = MetadataHelper.getEntity(entity);
		Field namedField = MetadataHelper.getNameField(entityMeta);
		
		// 默认配置
		if (cfgs == null) {
			columnList.add(formattedColumn(namedField));
			
			String namedFieldName = namedField.getName();
			if (!StringUtils.equalsIgnoreCase(namedFieldName, EntityHelper.createdBy)
					&& entityMeta.containsField(EntityHelper.createdBy)) {
				columnList.add(formattedColumn(entityMeta.getField(EntityHelper.createdBy)));
			}
			if (!StringUtils.equalsIgnoreCase(namedFieldName, EntityHelper.createdOn)
					&& entityMeta.containsField(EntityHelper.createdOn)) {
				columnList.add(formattedColumn(entityMeta.getField(EntityHelper.createdOn)));
			}
		} else {
			JSONArray config = (JSONArray) cfgs[1];
			for (Object o : config) {
				JSONObject col = (JSONObject) o;
				String field = col.getString("field");
				if (entityMeta.containsField(field)) {
					Map<String, Object> map = formattedColumn(entityMeta.getField(field));
					Integer width = col.getInteger("width");
					if (width != null) {
						map.put("width", width);
					}
					columnList.add(map);
				} else {
					LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
				}
			}
		}
		
		return JSONUtils.toJSONObject(
				new String[] { "entity", "nameField", "fields" }, 
				new Object[] { entity, namedField.getName(), columnList });
	}
	
	/**
	 * @param field
	 * @return
	 */
	public static Map<String, Object> formattedColumn(Field field) {
		EasyMeta easyMeta = new EasyMeta(field);
		return JSONUtils.toJSONObject(
				new String[] { "field", "label", "type" }, 
				new Object[] { easyMeta.getName(), easyMeta.getLabel(), easyMeta.getDisplayType(false) });
	}
}
