/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.configuration;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.NullValue;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.impl.CompatibleValueConversion;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

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
	    // @see field-edit.jsp 内建字段无配置
	    if (EasyMeta.valueOf(field).isBuiltin()) {
	        return JSONUtils.EMPTY_ARRAY;
        }

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
						sourceEntity.getField(sourceField),
                        targetEntity.getField(targetField),
						sourceRecord.getObjectValue(sourceField));
			}
			
			// NOTE 忽略空值
			if (value == null || NullValue.is(value) || StringUtils.isBlank(value.toString())) {
				continue;
			}
			
			ConfigEntry clone = e.clone().set("value", value);
			clone.set("source", null);
			fillin.add(clone.toJSON());
		}
		return fillin;
	}
	
	/**
	 * 回填值做兼容处理。例如引用字段回填至文本，要用 Label，而不是 ID
	 * 
	 * @param source
	 * @param target
	 * @param value
	 * @return
     * @see CompatibleValueConversion
	 */
	protected Object conversionCompatibleValue(Field source, Field target, Object value) {
	    return new CompatibleValueConversion(source, target).conversion(value, null, true);
	}
	
	/**
	 * 获取配置
	 * 
	 * @param field
	 * @return
	 */
	private List<ConfigEntry> getConfig(Field field) {
		final String cKey = "AutoFillinManager-" + field.getOwnEntity().getName() + "." + field.getName();
		Object cached = Application.getCommonCache().getx(cKey);
		if (cached != null) {
            //noinspection unchecked
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
	public void clean(Field field) {
		final String cKey = "AutoFillinManager-" + field.getOwnEntity().getName() + "." + field.getName();
		Application.getCommonCache().evict(cKey);
	}
}
