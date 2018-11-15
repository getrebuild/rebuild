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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单默认值
 * 
 * @author zhaofang123@gmail.com
 * @since 11/15/2018
 */
public class DefaultValueManager {
	
	private static final Log LOG = LogFactory.getLog(DefaultValueManager.class);

	/**
	 * @param entity
	 * @param formModel
	 * @param defaultVals
	 */
	public static void setFieldsValue(Entity entity, JSON formModel, JSON defaultVals) {
		Map<String, Object> valueReady = new HashMap<>();
		
		JSONObject fromClient = (JSONObject) defaultVals;
		for (Map.Entry<String, Object> e : fromClient.entrySet()) {
			String field = e.getKey();
			Object value = e.getValue();
			if (value == null || StringUtils.isBlank(value.toString())) {
				LOG.warn("Invalid field-value : " + field + " = " + value);
				continue;
			}
			
			// 引用字段实体
			if (field.startsWith("&")) {
				if (!ID.isId(value.toString())) {
					continue;
				}
				
				ID sourceRecord = ID.valueOf(value.toString());
				String recordLabel = FieldValueWrapper.getLabel(sourceRecord);
				final Object idLabel[] = new Object[] { sourceRecord.toLiteral(), recordLabel };
				
				Entity source = MetadataHelper.getEntity(field.substring(1));
				Field[] reftoFields = MetadataHelper.getReferenceToFields(source, entity);
				for (Field rtf : reftoFields) {
					valueReady.put(rtf.getName(), idLabel);
				}
			} else if (entity.containsField(field)) {
				// TODO ...
			}
		}
		
		if (valueReady.isEmpty()) {
			return;
		}
		
		JSONArray elements = ((JSONObject) formModel).getJSONArray("elements");
		for (Object o : elements) {
			JSONObject item = (JSONObject) o;
			String field = item.getString("field");
			if (valueReady.containsKey(field)) {
				item.put("value", valueReady.get(field));
			}
		}
	}
	
}
