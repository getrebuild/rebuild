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

package com.rebuild.server.business.rbstore;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigEntry;
import com.rebuild.server.configuration.portals.PickListManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * 元数据模型生成
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/28
 * 
 * @see MetaschemaImporter
 */
public class MetaSchemaGenerator {
	
	final private Entity entity;
	
	/**
	 * @param entity
	 */
	public MetaSchemaGenerator(Entity entity) {
		this.entity = entity;
	}
	
	/**
	 * @param dest
	 * @throws IOException 
	 */
	public void generate(File dest) throws IOException {
		JSON schema = generate();
		FileUtils.writeStringToFile(dest, JSON.toJSONString(schema, true), "utf-8");
	}
	
	/**
	 * @return
	 */
	public JSON generate() {
		JSONObject schema = (JSONObject) performEntity(entity, false);
		if (entity.getSlaveEntity() != null) {
			JSON slave = performEntity(entity.getSlaveEntity(), true);
			schema.put("slave", slave);
		}
		return schema;
	}
	
	/**
	 * @param entity
	 * @param isSlave
	 * @return
	 */
	private JSON performEntity(Entity entity, boolean isSlave) {
		JSONObject schemaEntity = new JSONObject(true);
		
		// 实体
		EasyMeta easyEntity = EasyMeta.valueOf(entity);
		schemaEntity.put("entity", entity.getName());
		schemaEntity.put("entityLabel", easyEntity.getLabel());
		if (easyEntity.getComments() != null) {
			schemaEntity.put("comments", easyEntity.getComments());
		}
		schemaEntity.put("nameField", entity.getNameField().getName());
		
		JSONArray metaFields = new JSONArray();
		for (Field field : entity.getFields()) {
			if (MetadataHelper.isCommonsField(field)
					|| (isSlave && MetadataHelper.getSlaveToMasterField(entity).equals(field))) {
				continue;
			}
			metaFields.add(performField(field));
		}
		schemaEntity.put("fields", metaFields);

		// 布局相关（仅管理员）
		JSONObject putLayouts = new JSONObject();
		Object layouts[][] = Application.createQueryNoFilter(
				"select applyType,config from LayoutConfig where belongEntity = ? and createdBy = ?")
				.setParameter(1, entity.getName())
				.setParameter(2, UserService.ADMIN_USER)
				.array();
		for (Object[] layout : layouts) {
			String type = (String) layout[0];
			JSONArray config = JSON.parseArray((String) layout[1]);
			if (!config.isEmpty()) {
				putLayouts.put(type, config);
			}
		}
		schemaEntity.put("layouts", putLayouts);
		
		// 过滤器（仅管理员）
		Object filters[][] = Application.createQueryNoFilter(
				"select filterName,config from FilterConfig where belongEntity = ? and createdBy = ?")
				.setParameter(1, entity.getName())
				.setParameter(2, UserService.ADMIN_USER)
				.array();
		JSONObject putFilters = new JSONObject();
		for (Object[] filter : filters) {
			String name = (String) filter[0];
			JSONObject config = JSON.parseObject((String) filter[1]);
			if (!config.isEmpty()) {
				putFilters.put(name, config);
			}
		}
		schemaEntity.put("filters", putFilters);
		
		return schemaEntity;
	}
	
	/**
	 * @param field
	 * @return
	 */
	private JSON performField(Field field) {
		final JSONObject schemaField = new JSONObject(true);
		final EasyMeta easyField = EasyMeta.valueOf(field);
		final DisplayType dt = easyField.getDisplayType();
		
		schemaField.put("field", easyField.getName());
		schemaField.put("fieldLabel", easyField.getLabel());
		schemaField.put("displayType", dt.name());
		if (easyField.getComments() != null) {
			schemaField.put("comments", easyField.getComments());
		}
		schemaField.put("nullable", field.isNullable());
		schemaField.put("updatable", field.isUpdatable());
		schemaField.put("repeatable", field.isRepeatable());
		Object defaultVal = field.getDefaultValue();
		if (defaultVal != null && StringUtils.isNotBlank((String) defaultVal)) {
			schemaField.put("defaultValue", defaultVal);
		}
		
		if (dt == DisplayType.REFERENCE) {
			schemaField.put("refEntity", field.getReferenceEntity().getName());
			schemaField.put("refEntityLabel", EasyMeta.getLabel(field.getReferenceEntity()));
		} else if (dt == DisplayType.PICKLIST) {
			schemaField.put("items", performPickList(field));
		}
		
		JSONObject extConfig = easyField.getFieldExtConfig();
		if (!extConfig.isEmpty()) {
			schemaField.put("extConfig", extConfig);
		}
		
		return schemaField;
	}
	
	/**
	 * @param field
	 * @return
	 */
	private JSON performPickList(Field field) {
		ConfigEntry entries[] = PickListManager.instance.getPickListRaw(
				field.getOwnEntity().getName(), field.getName(), false);
		JSONArray items = new JSONArray();
		for (ConfigEntry e : entries) {
			items.add(new Object[] { e.getString("text"), e.getBoolean("default") });
		}
		return items;
	}
}
