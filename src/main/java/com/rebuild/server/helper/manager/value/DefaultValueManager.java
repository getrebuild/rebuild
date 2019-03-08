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

package com.rebuild.server.helper.manager.value;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.cache.NoRecordFoundException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.web.IllegalParameterException;

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
	 * @param initialVal
	 */
	public static void setFieldsValue(Entity entity, JSON formModel, JSON initialVal) {
		final JSONArray elements = ((JSONObject) formModel).getJSONArray("elements");
		if (elements == null) {
			return;
		}
		
		Map<String, Object> valReady = new HashMap<>();
		
		// 客户端传递
		JSONObject fromClient = (JSONObject) initialVal;
		for (Map.Entry<String, Object> e : fromClient.entrySet()) {
			String field = e.getKey();
			Object value = e.getValue();
			if (value == null || StringUtils.isBlank(value.toString())) {
				LOG.warn("Invalid inital field-value : " + field + " = " + value);
				continue;
			}
			
			// 引用字段实体。&EntityName
			if (field.equals(DV_MASTER) || field.startsWith(DV_REFERENCE_PREFIX)) {
				Object idLabel[] = readyReferenceValue(value);
				
				if (field.equals(DV_MASTER)) {
					Field stm = MetadataHelper.getSlaveToMasterField(entity);
					valReady.put(stm.getName(), idLabel);
				} else {
					Entity source = MetadataHelper.getEntity(field.substring(1));
					Field[] reftoFields = MetadataHelper.getReferenceToFields(source, entity);
					for (Field rtf : reftoFields) {
						valReady.put(rtf.getName(), idLabel);
					}
				}
				
			} else if (entity.containsField(field)) {
				EasyMeta fieldMeta = EasyMeta.valueOf(entity.getField(field));
				if (fieldMeta.getDisplayType() == DisplayType.REFERENCE) {
					valReady.put(field, readyReferenceValue(value));
				}
				
				// TODO 填充其他字段值 ...
			} else {
				LOG.warn("Invalid inital field-value : " + field + " = " + value);
			}
		}
		
		// TODO 后台设置的，应该在后台处理 ???
		
		if (valReady.isEmpty()) {
			return;
		}
		
		for (Object o : elements) {
			JSONObject item = (JSONObject) o;
			String field = item.getString("field");
			if (valReady.containsKey(field)) {
				item.put("value", valReady.get(field));
				valReady.remove(field);
			}
		}
		
		// 还有没布局出来的也返回
		if (!valReady.isEmpty()) {
			JSONObject inital = new JSONObject();
			for (Map.Entry<String, Object> e : valReady.entrySet()) {
				Object v = e.getValue();
				if (v instanceof Object[]) {
					v = ((Object[]) v)[0].toString();
				}
				inital.put(e.getKey(), v);
			}
			((JSONObject) formModel).put("initialValue", inital);
		}
	}
	
	/**
	 * @param idVal
	 * @return
	 */
	private static Object[] readyReferenceValue(Object idVal) {
		if (!ID.isId(idVal.toString())) {
			throw new IllegalParameterException("Bad ID : " + idVal);
		}
		
		ID id = ID.valueOf(idVal.toString());
		String label = FieldValueWrapper.getLabel(id);
		if (label == null) {
			throw new NoRecordFoundException("No record found : " + idVal);
		}
		return new Object[] { id.toLiteral(), label };
	}
	
}
