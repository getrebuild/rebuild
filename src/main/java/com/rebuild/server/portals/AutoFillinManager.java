/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.DisplayType;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.portals.value.FieldValueWrapper;
import com.rebuild.utils.JSONUtils;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单自动回填
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/17
 */
public class AutoFillinManager implements PortalsManager {
	
	private static final Log LOG = LogFactory.getLog(AutoFillinManager.class);
	
	/**
	 * 获取回填值
	 * 
	 * @param field
	 * @param source
	 * @return
	 */
	public static JSONArray getFillinValue(Field field, ID source) {
		Map<String, ConfigEntry> config = getConfig(field);
		
		Entity sourceEntity = MetadataHelper.getEntity(source.getEntityCode());
		Set<String> sourceFields = new HashSet<>();
		for (ConfigEntry e : config.values()) {
			String sourceField = e.getString("source");
			if (!sourceEntity.containsField(sourceField)) {
				LOG.warn("Unknow field '" + sourceField + "' in '" + sourceEntity.getName() + "'");
				continue;
			} 
			
			Field sourceField2 = sourceEntity.getField(sourceField);
			if (EasyMeta.getDisplayType(sourceField2) == DisplayType.REFERENCE) {
				sourceFields.add("&" + sourceField);
			}
			sourceFields.add(sourceField);
		}
		if (sourceFields.isEmpty()) {
			return JSONUtils.EMPTY_ARRAY;
		}
		
		String ql = String.format("select %s from %s where %s = ?",
				StringUtils.join(sourceFields, ","),
				sourceEntity.getName(),
				sourceEntity.getPrimaryField().getName());
		Record sourceRecord = Application.createQuery(ql).setParameter(1, source).record();
		if (sourceRecord == null) {
			return JSONUtils.EMPTY_ARRAY;
		}
		
		JSONArray fillin = new JSONArray();
		for (ConfigEntry e : config.values()) {
			String sourceField = e.getString("source");
			Object formatted = null;
			if (sourceRecord.hasValue(sourceField)) {
				Object v = sourceRecord.getObjectValue(sourceField);
				if (v instanceof ID) {
					v = new Object[] { v, ((ID) v).getLabel() };
				}
				formatted = FieldValueWrapper.wrapFieldValue(v, sourceEntity.getField(sourceField));
			}
			
			ConfigEntry clone = e.clone().set("value", formatted == null ? "" : formatted);
			clone.set("source", null);
			fillin.add(clone.toJSON());
		}
		return fillin;
	}
	
	/**
	 * @param field
	 * @return
	 */
	private static Map<String, ConfigEntry> getConfig(Field field) {
		Object[][] array = Application.createQueryNoFilter(
				"select sourceField,targetField,extConfig from AutoFillinConfig where belongEntity = ? and belongField = ?")
				.setParameter(1, field.getOwnEntity().getName())
				.setParameter(2, field.getName())
				.array();
		
		Map<String, ConfigEntry> entries = new HashMap<>();
		for (Object[] o : array) {
			ConfigEntry entry = new ConfigEntry()
					.set("source", o[0])
					.set("target", o[1]);
			JSONObject ext = JSON.parseObject((String) o[2]);
			entry.set("whenCreate", ext.getBoolean("whenCreate"))
					.set("whenUpdate", ext.getBoolean("whenUpdate"))
					.set("fillinForce", ext.getBoolean("fillinForce"));
			entries.put((String) o[1], entry);
		}
		return entries;
	}
}
