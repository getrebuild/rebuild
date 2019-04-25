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

package com.rebuild.server.portals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
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
public class DataListManager extends BaseLayoutManager {

	/**
	 * @param entity
	 * @param user
	 * @return
	 */
	public static JSON getColumnLayout(String entity, ID user) {
		Object[] config = getLayoutOfDatalist(user, entity);
		
		List<Map<String, Object>> columnList = new ArrayList<>();
		Entity entityMeta = MetadataHelper.getEntity(entity);
		Field namedField = MetadataHelper.getNameField(entityMeta);
		
		// 默认配置
		if (config == null) {
			columnList.add(formattedColumn(namedField));
			
			String namedFieldName = namedField.getName();
			if (!StringUtils.equalsIgnoreCase(namedFieldName, EntityHelper.CreatedBy)
					&& entityMeta.containsField(EntityHelper.CreatedBy)) {
				columnList.add(formattedColumn(entityMeta.getField(EntityHelper.CreatedBy)));
			}
			if (!StringUtils.equalsIgnoreCase(namedFieldName, EntityHelper.CreatedOn)
					&& entityMeta.containsField(EntityHelper.CreatedOn)) {
				columnList.add(formattedColumn(entityMeta.getField(EntityHelper.CreatedOn)));
			}
		} else {
			for (Object item : (JSONArray) config[1]) {
				JSONObject column = (JSONObject) item;
				String field = column.getString("field");
				String fieldPaths[] = field.split("\\.");
				if (!entityMeta.containsField(fieldPaths[0])) {
					LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
					continue;
				}
				
				Field fieldMeta = entityMeta.getField(fieldPaths[0]);
				Map<String, Object> formatted = null;
				if (fieldPaths.length == 1) {
					formatted = formattedColumn(fieldMeta);
				} else {
					Entity refEntity = fieldMeta.getReferenceEntity();
					if (refEntity != null && refEntity.containsField(fieldPaths[1])) {
						formatted = formattedColumn(refEntity.getField(fieldPaths[1]), fieldMeta);
					} else {
						LOG.warn("Unknow field '" + field + "' in '" + entity + "'");
					}
				}
				
				if (formatted != null) {
					Integer width = column.getInteger("width");
					if (width != null) {
						formatted.put("width", width);
					}
					columnList.add(formatted);
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
		return formattedColumn(field, null);
	}
	
	/**
	 * @param field
	 * @param parent
	 * @return
	 */
	public static Map<String, Object> formattedColumn(Field field, Field parent) {
		String parentField = parent == null ? "" : (parent.getName() + ".");
		String parentLabel = parent == null ? "" : (EasyMeta.getLabel(parent) + ".");
		EasyMeta easyField = new EasyMeta(field);
		return JSONUtils.toJSONObject(
				new String[] { "field", "label", "type" },
				new Object[] { parentField + easyField.getName(), parentLabel + easyField.getLabel(), easyField.getDisplayType(false) });
	}
	
	/**
	 * @param user
	 * @param belongEntity
	 * @return
	 */
	public static ID detectUseConfig(ID user, String belongEntity) {
		return detectUseConfig(user, "LayoutConfig", belongEntity, TYPE_DATALIST);
	}
}
