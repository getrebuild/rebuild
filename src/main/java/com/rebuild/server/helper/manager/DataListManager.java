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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;

/**
 * 数据列表
 * 
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
public class DataListManager extends LayoutManager {
	
	private static final Log LOG = LogFactory.getLog(DataListManager.class);

	/**
	 * @param entity
	 * @return
	 */
	public static JSON getColumnLayout(String entity) {
		Object[] raw = getLayoutConfigRaw(entity, TYPE_DATALIST);
		
		List<Map<String, Object>> columnList = new ArrayList<>();
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		Field nameField = entityMeta.getNameField();
		nameField = nameField == null ? entityMeta.getPrimaryField() : nameField;
		
		// 默认配置
		if (raw == null) {
			if (nameField != null) {
				columnList.add(warpColumn(nameField));
			}
			if (entityMeta.containsField(EntityHelper.createdBy)) {
				columnList.add(warpColumn(entityMeta.getField(EntityHelper.createdBy)));
			}
		} else {
			JSONArray config = (JSONArray) raw[1];
			for (Object o : config) {
				JSONObject jo = (JSONObject) o;
				String field = jo.getString("field");
				if (entityMeta.containsField(field)) {
					Map<String, Object> map = warpColumn(entityMeta.getField(field));
					Integer width = jo.getInteger("width");
					if (width != null) {
						map.put("width", width);
					}
					columnList.add(map);
				} else {
					LOG.warn("Invalid field : " + field);
				}
			}
		}
		
		Map<String, Object> ret = new HashMap<>();
		ret.put("entity", entity);
		ret.put("nameField", nameField.getName());
		ret.put("fields", columnList);
		return (JSON) JSON.parse(JSON.toJSONString(ret));
	}
	
	/**
	 * @param field
	 * @return
	 */
	public static Map<String, Object> warpColumn(Field field) {
		EasyMeta easyMeta = new EasyMeta(field);
		Map<String, Object> map = new HashMap<>();
		map.put("field", easyMeta.getName());
		map.put("label", easyMeta.getLabel());
		map.put("type", easyMeta.getDisplayType(false));
		return map;
	}
}
