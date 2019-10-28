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

package com.rebuild.server.configuration;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 表单自动回填
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/17
 */
public class AutoFillinManager implements ConfigManager<Field> {
	
	private static final Log LOG = LogFactory.getLog(AutoFillinManager.class);
	
	public static final AutoFillinManager instance = new AutoFillinManager();
	private AutoFillinManager() { }
	
	/**
	 * 获取回填值
	 * 
	 * @param field
	 * @param source
	 * @return
	 */
	public JSONArray getFillinValue(Field field, ID source) {
		final List<ConfigEntry> config = getConfig(field);
		if (config.isEmpty()) {
			return JSONUtils.EMPTY_ARRAY;
		}
		
		Entity sourceEntity = MetadataHelper.getEntity(source.getEntityCode());
		Entity targetEntity = field.getOwnEntity();
		Set<String> sourceFields = new HashSet<>();
		for (ConfigEntry e : config) {
			String sourceField = e.getString("source");
			String targetField = e.getString("target");
			if (!MetadataHelper.checkAndWarnField(sourceEntity, sourceField)
					|| !MetadataHelper.checkAndWarnField(targetEntity, targetField)) {
				continue;
			}

			Field sourceFieldMeta = sourceEntity.getField(sourceField);
			if (EasyMeta.getDisplayType(sourceFieldMeta) == DisplayType.REFERENCE) {
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
		Record sourceRecord = Application.createQueryNoFilter(ql).setParameter(1, source).record();
		if (sourceRecord == null) {
			return JSONUtils.EMPTY_ARRAY;
		}
		
		JSONArray fillin = new JSONArray();
		for (ConfigEntry e : config) {
			String sourceField = e.getString("source");
			Object value = null;
			if (sourceRecord.hasValue(sourceField)) {
				String targetField = e.getString("target");
				value = conversionCompatibleValue(
						sourceEntity.getField(sourceField), targetEntity.getField(targetField),
						sourceRecord.getObjectValue(sourceField));
			}
			
			// NOTE 忽略空值
			if (value == null || StringUtils.isBlank(value.toString())) {
				continue;
			}
			
			ConfigEntry clone = e.clone().set("value", value);
			clone.set("source", null);
			fillin.add(clone.toJSON());
		}
		return fillin;
	}
	
	/**
	 * 回填值做兼容处理。例如 引用字段回填至文本，要用 Label，而不是 ID 数组
	 * 
	 * @param source
	 * @param target
	 * @param value
	 * @return
	 */
	protected Object conversionCompatibleValue(Field source, Field target, Object value) {
		DisplayType sourceType = EasyMeta.getDisplayType(source);
		DisplayType targetType = EasyMeta.getDisplayType(target);
		boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
		
		Object compatibleValue = null;
		if (sourceType == DisplayType.REFERENCE) {
			if (is2Text) {
				compatibleValue = ((ID) value).getLabel();
			} else {
				Object[] idAndLabel = new Object[] { value, ((ID) value).getLabel() };
				compatibleValue = FieldValueWrapper.instance.wrapFieldValue(idAndLabel, source);
			}
		} else if (sourceType == DisplayType.CLASSIFICATION) {
			// Label
			compatibleValue = FieldValueWrapper.instance.wrapFieldValue(value, source);
			if (!is2Text) {
				compatibleValue = new Object[] { value, compatibleValue };  // [ID, Label]
			}
		} else if (sourceType == DisplayType.PICKLIST || sourceType == DisplayType.STATE) {
			if (is2Text) {
				compatibleValue = FieldValueWrapper.instance.wrapFieldValue(value, source);
			} else {
				compatibleValue = value;
			}
		} else if (sourceType == DisplayType.DATETIME && targetType == DisplayType.DATE) {
			String datetime = (String) FieldValueWrapper.instance.wrapFieldValue(value, source);
			compatibleValue = datetime.split(" ")[0];
		} else if (sourceType == DisplayType.DATE && targetType == DisplayType.DATETIME) {
			String date = (String) FieldValueWrapper.instance.wrapFieldValue(value, source);
			compatibleValue = date + " 00:00:00";
		} else {
			compatibleValue = FieldValueWrapper.instance.wrapFieldValue(value, source);
		}
		
		return compatibleValue;
	}
	
	/**
	 * 获取配置
	 * 
	 * @param field
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<ConfigEntry> getConfig(Field field) {
		final String cKey = "AutoFillinManager-" + field.getOwnEntity().getName() + "." + field.getName();
		Object cached = Application.getCommonCache().getx(cKey);
		if (cached != null) {
			return (List<ConfigEntry>) cached;
		}
		
		Object[][] array = Application.createQueryNoFilter(
				"select sourceField,targetField,extConfig from AutoFillinConfig where belongEntity = ? and belongField = ?")
				.setParameter(1, field.getOwnEntity().getName())
				.setParameter(2, field.getName())
				.array();
		
		ArrayList<ConfigEntry> entries = new ArrayList<>();
		for (Object[] o : array) {
			ConfigEntry entry = new ConfigEntry()
					.set("source", o[0])
					.set("target", o[1]);
			JSONObject ext = JSON.parseObject((String) o[2]);
			entry.set("whenCreate", ext.getBoolean("whenCreate"))
					.set("whenUpdate", ext.getBoolean("whenUpdate"))
					.set("fillinForce", ext.getBoolean("fillinForce"));
			entries.add(entry);
		}
		
		Application.getCommonCache().putx(cKey, entries);
		return entries;
	}
	
	@Override
	public void clean(Field cacheKey) {
		final String cKey = "AutoFillinManager-" + cacheKey.getOwnEntity().getName() + "." + cacheKey.getName();
		Application.getCommonCache().evict(cKey);
	}
}
