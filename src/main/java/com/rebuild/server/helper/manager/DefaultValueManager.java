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
import com.rebuild.server.entityhub.DisplayType;
import com.rebuild.server.entityhub.EasyMeta;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BadParameterException;

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
	
	public static final String DV_MASTER = "$MASTER$";
	public static final String DV_REFERENCE_PREFIX = "&";

	/**
	 * @param entity
	 * @param formModel
	 * @param defaultVals
	 */
	public static void setFieldsValue(Entity entity, JSON formModel, JSON defaultVals) {
		final JSONArray elements = ((JSONObject) formModel).getJSONArray("elements");
		// Invalid Model
		if (elements == null) {
			return;
		}
		
		Map<String, Object> valuesReady = new HashMap<>();
		
		// 客户端传递
		JSONObject fromClient = (JSONObject) defaultVals;
		for (Map.Entry<String, Object> e : fromClient.entrySet()) {
			String field = e.getKey();
			Object value = e.getValue();
			if (value == null || StringUtils.isBlank(value.toString())) {
				LOG.warn("Invalid field-value : " + field + " = " + value);
				continue;
			}
			
			// 引用字段实体。&EntityName
			if (field.equals(DV_MASTER) || field.startsWith(DV_REFERENCE_PREFIX)) {
				Object idLabel[] = readyReferenceValue(value);
				
				if (field.equals(DV_MASTER)) {
					Field stm = MetadataHelper.getSlaveToMasterField(entity);
					valuesReady.put(stm.getName(), idLabel);
				} else {
					Entity source = MetadataHelper.getEntity(field.substring(1));
					Field[] reftoFields = MetadataHelper.getReferenceToFields(source, entity);
					for (Field rtf : reftoFields) {
						valuesReady.put(rtf.getName(), idLabel);
					}
				}
				
			} else if (entity.containsField(field)) {
				EasyMeta fieldMeta = EasyMeta.valueOf(entity.getField(field));
				if (fieldMeta.getDisplayType() == DisplayType.REFERENCE) {
					valuesReady.put(field, readyReferenceValue(value));
				}
				
				// TODO 填充其他字段值 ...
			}
		}
		
		// TODO 后台设置的，应该在后台处理 ???
		
		if (valuesReady.isEmpty()) {
			return;
		}
		
		for (Object o : elements) {
			JSONObject item = (JSONObject) o;
			String field = item.getString("field");
			if (valuesReady.containsKey(field)) {
				item.put("value", valuesReady.get(field));
				valuesReady.remove(field);
			}
		}
	}
	
	/**
	 * @param idVal
	 * @return
	 */
	private static Object[] readyReferenceValue(Object idVal) {
		if (!ID.isId(idVal.toString())) {
			throw new BadParameterException("Bad ID : " + idVal);
		}
		
		ID id = ID.valueOf(idVal.toString());
		String label = FieldValueWrapper.getLabel(id);
		if (label == null) {
			throw new NoRecordFoundException("No record found : " + idVal);
		}
		return new Object[] { id.toLiteral(), label };
	}
	
}
